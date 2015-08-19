/*
This source file is part of Smyle, a database library.
For up-to-date information, see http://www.drjava.de/smyle
Copyright (C) 2001 Stefan Reich (doc@drjava.de)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For full license text, see doc/license/lgpl.txt in this distribution
*/

package drjava.smyle.core;

import java.io.*;
import java.util.*;
import java.lang.Object;
import java.lang.ref.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import org.artsProject.util.*;
import drjava.gjutil.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;
import drjava.smyle.core.indexing.*;

public final class TableImpl<T extends Struct<T>>
  implements Table<T>, UntypedTable {
  final SnapshotImpl snapshot;
  final ChunkManager chunkManager;
  ChunkRef schemaChunk = ChunkManager.NULLCHUNK;
  FastIntVector elements = new FastIntVector();
  Demarshaller<T> demarshaller;
  StructInfo<T> type;
  FunctionMarDemar<T> fmd;
  int id;
  boolean mutable = true, dirty = false;
  String whyImmutable;
  transient int modCount = 0;
  
  int maxKeyLength = 16;
  UniversalKey maxKey;
  ArrayList<Index<T>> indexes = new ArrayList<Index<T>>();
  IndexAdvisor<Pair<IndexProfile,Function>> indexAdvisor = null;

  Field<T,Integer> autoincField = null;
  int autoincCounter = 0;
  
  /** maximum size of an index tree leaf */
  static final int INDEX_M = 20;
    
  static final boolean
    debug = false,
    profiling = false;
  
  static final StructInfo UNTYPED = new StructInfo(null, null, null, null);
  
  private <U extends Struct<U>> Demarshaller<U> constructCustomDemarshaller(StructInfo existingType, StructInfo<U> type,
    HashMap<Pair<StructInfo,StructInfo>,FieldBasedDemarshaller> catalog) throws SchemaChangeException {
    Pair<StructInfo,StructInfo> catalogKey = new Pair<StructInfo,StructInfo>(existingType, type);
    FieldBasedDemarshaller<U> demarshaller = catalog.get(catalogKey);
    if (demarshaller != null) return demarshaller;
      
    demarshaller = new FieldBasedDemarshaller<U>(type);
    catalog.put(catalogKey, demarshaller);
    
    int removedFields = 0;
    for (int i = 0; i < existingType.typeDef.contents.size(); i++) {
      TypeComponent c = existingType.typeDef.contents.get(i);
      int iNew = type.findTypeComponent(c.name);
      if (iNew < 0) {
        mutable = false;
        if (whyImmutable != null)
          whyImmutable += ", "+c.name;
        else
          whyImmutable = "Missing field(s): "+c.name;
        ++removedFields;
      } else {
        TypeComponent cNew = type.typeDef.contents.get(iNew);
        if (!cNew.type.equals(c.type)) {
          if (existingType.fieldTypes[i] != null) {
            /*if (debug) {
              System.out.println("Demarshaller field "+existingType.fieldTypes[i].typeDef+" -> "+(type.fieldTypes[iNew] != null ? type.fieldTypes[iNew].typeDef : null));
              System.out.println("New type: "+cNew.type);
            }*/
            Demarshaller d = constructCustomDemarshaller(
              existingType.fieldTypes[i],
              type.fieldTypes[iNew],
              catalog);
            if (cNew.type.startsWith("*"))
              demarshaller.addSequenceField(iNew, d);
            else
              demarshaller.addDemarshallerField(iNew, d);
            continue;
          }
          throw new SchemaChangeException("Field "+c.name+" had type "+c.type+"; now "+cNew.type+"\n"
          +"Old schema: "+existingType.typeDef+"\n"
          +"New schema: "+type.typeDef);
        }
      }
          
      if (c.type.equals("long"))
        demarshaller.addLongField(iNew);
      else if (c.type.equals("long long"))
        demarshaller.addLongLongField(iNew);
      else if (c.type.equals("boolean"))
        demarshaller.addBooleanField(iNew);
      else if (c.type.equals("string"))
        demarshaller.addStringField(iNew);
      else if (c.type.startsWith("*"))
        if (type.fieldTypes[iNew] != null)
          demarshaller.addSequenceField(iNew, type.fieldTypes[iNew].demarshaller);
        else if (c.type.equals("*string"))
          demarshaller.addStringSequenceField(iNew);
        else if (c.type.equals("*byte"))
          demarshaller.addByteSequenceField(iNew);
        else if (c.type.equals("*long"))
          demarshaller.addLongSequenceField(iNew);
        else
          throw new RuntimeException("TODO: "+c.type);
      else if (type.fieldTypes[iNew] == null)
        demarshaller.addLongField(iNew); // enum
      else
        demarshaller.addStructField(iNew, type.fieldTypes[iNew]);
    }
    
    // adding and removing fields at the same time is not allowed
    if (removedFields != 0 && type.typeDef.contents.size() != existingType.typeDef.contents.size()-removedFields) {
      throw new SchemaChangeException("Adding and removing fields at the same time is not allowed");
    }
    
    return demarshaller;
  }
  
  /** create an untyped table */
  TableImpl(SnapshotImpl snapshot, int id, ChunkRef tableChunk, String name) {
    this(snapshot, UNTYPED, id, tableChunk, name);
  }
  
  /** create a typed table */
  TableImpl(SnapshotImpl snapshot, StructInfo<T> type, int id, ChunkRef tableChunk, String name) {
    this.snapshot = snapshot;
    chunkManager = snapshot.store.chunkManager;
    int version = snapshot.store.version;
    this.type = type;
    fmd = new FunctionMarDemar<T>(type);
    this.demarshaller = type.demarshaller;
    TypeDef schema = type.typeDef;
    this.id = id;

    setMaxKeyLength(maxKeyLength);
    
    // read elements file
    
    try {
      //if (debug) System.out.println("tableChunk: "+tableChunk.id);
      if (tableChunk.index != 0) {
        Buffer b = chunkManager.readChunk(tableChunk);
        if (version >= 2)
          schemaChunk = new ChunkRef(b);
        elements.read(b);
        
        // load index
        
        if (version >= 281) {
          // load autoincrement field
          
          if (version >= 27) {
            int fieldNr = b.readLong();
            if (fieldNr >= 0) {
              autoincField = (Field)
                new FunctionMarDemar(type).getField(fieldNr);
              autoincCounter = b.readLong();
            }
          }
          
          if (version >= 890) {
            int numIndexes = b.readLong();
            for (int i = 0; i < numIndexes; i++)
              indexes.add(new Index(this, b));
          }
        }
      }
        
      StructInfo existingType = loadType();
      TypeDef existingSchema = existingType != null ? existingType.typeDef : null;
      //if (debug) System.out.println("version: "+version+", schema: "+existingSchema+", new schema: "+schema);
      
      // convert to new chunk manager
      if (chunkManager != snapshot.newChunkManager) {
        schemaChunk = snapshot.reStoreChunk(schemaChunk);
        for (int i = 0; i < elements.size(); i++) {
          elements.set(i, snapshot.reStoreChunk(new ChunkRef(elements.get(i))).index);
          if (i % 100 == 0 && i != 0)
            log(getName()+": "+i+" elements converted");
        }
      }
      
      // Check for compatibility
      // TODO: this is a mess - refactor!
      boolean schemaChange = 
        existingType != null && type != null && existingType.typeDef != null && type.typeDef != null
        && !existingType.equals(type);
      /*if (schemaChange) {
        System.out.println("old schema: "+(existingType != null ? existingType.typeDef : null));
        System.out.println("new schema: "+(type != null ? type.typeDef : null));
      }*/
      if (elements.size() != 0 && schemaChange) {
        // throws SchemaChangeException if it fails
        demarshaller = constructCustomDemarshaller(existingType, type,
          new HashMap<Pair<StructInfo,StructInfo>,FieldBasedDemarshaller>());
        
        clearIndexes();
            
        // convert table to new schema
        for (int i = 0; i < elements.size(); i++) {
          T t = loadElement(i);
          //if (debug) System.out.println("Element read: "+t);
          saveElement(t, i);
        }
        
        // use new schema's native demarshaller from now on
        demarshaller = type.demarshaller;
      }
      
      if (schema != null && (existingSchema == null || schemaChange)) {
        saveSchema();
        if (debug) System.out.println("Saved schema: "+schemaChunk);
      }
      
      // for untyped case
      if (this.type == null || this.type.typeDef == null) this.type = existingType;

      //if (debug) System.out.println("Final schema: "+(this.type != null ? this.type.typeDef : null));
      
      indexAdvisor = snapshot.store.getIndexAdvisor(name, type);
      buildIndexIfAdvised(); // this must be done somewhere
    } catch (MCOPException e) {
      e.printStackTrace();
      throw new InternalSmyleError(e);
    }
  }
  
  void flush() { synchronized(snapshot) { try { enter("flush");
    //System.out.println("Table "+id+".flush: dirty="+dirty);
    if (dirty) {
      // write table chunk
      Buffer b = new Buffer();
      schemaChunk.writeType(b);
      elements.marshal(b);
      
      // save autoincrement field
      
      if (autoincField != null) {
        b.writeLong(autoincField.nr());
        b.writeLong(autoincCounter);
      } else
        b.writeLong(-1);
        
      // save index
      
      MCOP.writeSeq(b, indexes);
        
      ChunkRef chunk = snapshot.newChunkManager.createChunk(b);
      //if (debug) System.out.println("Wrote "+elements.size()+" elements to chunk "+chunk+", schemaChunk="+schemaChunk);
      snapshot.setTableChunk(id, chunk);
      dirty = false;
    }
  } finally { leave(); }}}
  
  private void assertMutable() {
    snapshot.assertMutable();
    if (!mutable)
      throw new ImmutableTableException(whyImmutable);
  }
  
  public boolean add(T t) { synchronized(snapshot) { try { enter("add");
    assertMutable();
    ++modCount;
    int chunk = saveElement(t);
    elements.add(chunk);
    return true;
  } finally { leave(); }}}
    
  public void add(int index, T t) { synchronized(snapshot) { try { enter("add");
    assertMutable();
    ++modCount;
    elements.add(index, 0);
    saveElement(t, index);
  } finally { leave(); }}}
  
  private void saveElement(T t, int index) {
    elements.set(index, saveElement(t));
  }
  
  private int saveElement(T t) {
    // check element type
    
    if (!type.structClass.isInstance(t)) {
      if (t == null)
        throw new NullPointerException("Null elements are not allowed");
      throw new ClassCastException("Bad element type: "+t.getClass().getName()
        +", expected "+type.structClass.getName());
    }
    
    // auto increment handling
    
    if (autoincField != null) {
      int value = autoincField.of(t).intValue();
      if (value == 0)
        autoincField.set(t, new Integer(++autoincCounter));
      else
        autoincCounter = Math.max(autoincCounter, value);
    }
    
    // save to chunk
    
    ChunkRef chunk = snapshot.newChunkManager.createChunk(t);
    addToIndex(t, chunk);
    dirty = true;
    return chunk.index;
  }
  
  public boolean addAll(Collection<T> c) { synchronized(snapshot) { try { enter("addAll");
    return addAll(elements.size(), c);
  } finally { leave(); }}}
  
  public boolean addAll(int index, Collection<T> c) { synchronized(snapshot) { try { enter("addAll");
    assertMutable();
    boolean changed = false;
    for (Iterator<T> i = c.iterator(); i.hasNext(); ) {
      changed = true;
      add(index++, i.next());
    }
    return changed;
  } finally { leave(); }}}
  
  public boolean isEmpty() { synchronized(snapshot) { try { enter("isEmpty");
    return size() == 0;
  } finally { leave(); }}}
  
  public T loadElement(int i) { synchronized(snapshot) { try { enter("loadElement");
    try {
      int chunk = elements.get(i);
      //if (debug) System.out.println("loadElement #"+i+" fr="+fr.id);
      return demarshaller.read(chunkManager.readChunk(chunk));
    } catch (MCOPException e) {
      throw new InternalSmyleError(e);
    }
  } finally { leave(); }}}
  
  public T loadElement(ChunkRef chunk) { synchronized(snapshot) { try { enter("loadElement");
    return loadElementChunk(chunk.index);
  } finally { leave(); }}}
  
  public T loadElementChunk(int chunk) { synchronized(snapshot) { try { enter("loadElementChunk");
    try {
      return demarshaller.read(chunkManager.readChunk(chunk));
    } catch (MCOPException e) {
      System.err.println("chunk="+chunk);
      throw new InternalSmyleError(e);
    }
  } finally { leave(); }}}
  
  public T first() { synchronized(snapshot) { try { enter("first");
    if (elements.isEmpty())
      return null;
    return loadElement(0);
  } finally { leave(); }}}
  
  public T get(int nr) { synchronized(snapshot) { try { enter("get(int)");
    return loadElement(nr);
  } finally { leave(); }}}
  
  public int size() { synchronized(snapshot) { try { enter("size");
    return elements.size();
  } finally { leave(); }}}
  
  public void clear() { synchronized(snapshot) { try { enter("clear");
    assertMutable();
    elements.clear();
    clearIndexes();
    ++modCount;
    dirty = true;
  } finally { leave(); }}}
  
  public void removeAll(Filter<T> filter) { synchronized(snapshot) { try { enter("removeAll");
    assertMutable();    
    ArrayList<ChunkRef> list = new ArrayList<ChunkRef>();
    
    TableIterator<T> it = iteratorImpl(filter);    
    while (it.hasNext())
      list.add(it.nextChunk());

    //System.out.println("ChunkRefs for "+clause.getValue()+": "+list);
    for (int i = 0; i < list.size(); i++)
      removeChunk(list.get(i).index);

    buildIndexIfAdvised();
  } finally { leave(); }}}
  
  UniversalKey cutKey(UniversalKey key) {
    return key.cut(maxKeyLength);
  }

  public boolean contains(Filter<T> filter) { synchronized(snapshot) { try { enter("contains(Filter)");
    return iterator(filter).hasNext();
  } finally { leave(); }}}
  
  public boolean contains(T t) { synchronized(snapshot) { try { enter("contains(T)");
    return iterator(elementToFilter(t)).hasNext();
  } finally { leave(); }}}
  
  public int count(Filter<T> filter) { synchronized(snapshot) { try { enter("count");
    int count = 0;
    for (TableIterator<T> i = iteratorImpl(filter); i.hasNext(); ) {
      i.nextChunk();
      ++count;
    }
    return count;
  } finally { leave(); }}}
  
  public int indexOf(Filter<T> filter) { synchronized(snapshot) { try { enter("indexOf(Filter)");
    TableIterator<T> i = iteratorImpl(filter);
    if (!i.hasNext()) return -1;
    int result = Integer.MAX_VALUE;
    do {
      ChunkRef cr = i.nextChunk();
      int idx = elements.indexOf(cr.index);
      if (idx < result) result = idx;
    } while (i.hasNext());
    return result;
  } finally { leave(); }}}
  
  public T get(Filter<T> filter) { synchronized(snapshot) { try { enter("get(Filter)");
    Iterator<T> i = iterator(filter);
    return i.hasNext() ? i.next() : null;
  } finally { leave(); }}}
  
  public T getOrCreate(Filter<T> filter) { synchronized(snapshot) { try { enter("getOrCreate");
    T t = get(filter);
    return t != null ? t : filter._createMatchingElement();
  } finally { leave(); }}}
  
  public void put(Filter<T> filter, T t) { synchronized(snapshot) { try { enter("put");
    int idx = indexOf(filter);
    if (idx >= 0)
      set(idx, t);
    else
      add(t);
  } finally { leave(); }}}
  
  public List<T> subList(Filter<T> filter) { synchronized(snapshot) { try { enter("subList");
    ArrayList<ChunkRef> chunks = new ArrayList<ChunkRef>();
    for (TableIterator<T> i = iteratorImpl(filter); i.hasNext(); )
      chunks.add(i.nextChunk());
    buildIndexIfAdvised();
    return new SubList<T>(this, chunks);
  } finally { leave(); }}}
  
  private <A> void addIfIndexable(ArrayList<Function> v, Function<T,A> f) {
    if (isIndexable(f) && !v.contains(f))
      v.add(f);
  }
  
  private <A> boolean isIndexable(Function<T,A> f) {
    if (f instanceof Field) {
      Field field = (Field) f;
      
      // indexable type?
      String t = type.typeDef.contents.get(field.nr()).type;
      if (t.equals("long") || t.equals("long long") || t.equals("boolean") || t.equals("string"))
        return true;
    } else if (f instanceof Cascade && ((Cascade) f).g() instanceof ToLowerCase) {
      //if (debug) System.out.println("indexable cascade: "+f);
      return true;
    }
    return false;
  }
  
  public Iterator<T> iterator(Filter<T> filter) { synchronized(snapshot) { try { enter("iterator");
    return iteratorImpl(filter);
  } finally { leave(); }}}
  
  private TableIterator<T> iteratorImpl(Filter<T> filter) {
    // no filter at all => table scan
    if (filter == null)
      return new TableScan<T>(this, filter);
      
    // gather some information agbout the filter
    IndexProfile profile = QueryOptimizer.createProfile(filter);
    boolean ordered = !filter._getOrder().isEmpty();

    // find best fitting index
    int bestScore = 0;
    Index<T> bestIndex = null;
    
    for (int i = 0; i < indexes.size(); i++) {
      Index<T> idx = indexes.get(i);
      int score = QueryOptimizer.indexScore(profile, idx.fields);
      if (score > bestScore) {
        bestScore = score;
        bestIndex = idx;
      }
    }
    
    //System.out.println("bestIndex: "+bestIndex);
    if (bestIndex == null) {
      // No matching index, table scan
      
      if (ordered) {
        // sorted table scan, inform index advisor
        if (indexAdvisor != null) {
          if (debug) System.out.println("Informing index advisor, profile="+profile+", field="+filter._getOrder().get(0));
          indexAdvisor.queryPerformed(new Pair<IndexProfile,Function>[] { 
            new Pair<IndexProfile,Function>(profile, filter._getOrder().get(0)) },
            size());
          buildIndexIfAdvised();
        }
        
        try { enter("sorted table scan");
          if (debug) System.out.println("sorted table scan over "+getName()+" (profile="+profile+")");
          return new SortedIterator<T>(this, new TableScan<T>(this, filter), filter);
        } finally { leave(); }
      } else {
        try { enter("simple table scan");
          return new TableScan<T>(this, filter);
        } finally { leave(); }
      }
    }
    
    // We found a matching index; generate key prefix
    
    UniversalKey keyPrefix = bestIndex.makeKeyPrefix(filter);
    KeySet<UniversalKey> keySet = new PrefixKeySet(keyPrefix);
    TableIterator<T> iterator;
      
    try { enter("new IndexScan");
      iterator = new IndexScan<T>(this, filter, bestIndex, keySet);
    } finally { leave(); }
    /*int idx;
    if (filter._getOrder().size() == 1 && (idx = indexedFields.indexOf(filter._getOrder().get(0))) >= 0) {
      try { enter("sortByIndexField");
        iterator = is.sortByIndexField(idx);
      } finally { leave(); }
    } else*/
    if (ordered)
      try { enter("ordered");
        iterator = new SortedIterator<T>(this, iterator, filter); 
      } finally { leave(); }
    return iterator;
  }
  
  public Iterator<T> iterator() { synchronized(snapshot) { try { enter("iterator");
    return iterator(null);
  } finally { leave(); }}}
  
  public Iterator<T> iterator(final Filter<T> filter,
    final BTree<UniversalKey,ChunkRef> index) { synchronized(snapshot) { try { enter("iterator");
    class I implements Iterator<T> {
      Iterator<ChunkRef> i;
      
      I() {
        i = index.iterate();
      }
      
      public boolean hasNext() {
        return i.hasNext();
      }
      
      public T next() {
        return loadElement(i.next());
      }
      
      public void remove() {
        throw new RuntimeException("TODO");
      }
    };
    return new I();
  } finally { leave(); }}}
  
  public boolean remove(T t) { synchronized(snapshot) { try { enter("remove");
    assertMutable();
    int i = indexOf(t);
    if (i >= 0) {
      remove(i);
      return true;
    }
    return false;
  } finally { leave(); }}}
  
  public T remove(int index) { synchronized(snapshot) { try { enter("remove");
    assertMutable();
    T t = loadElement(index);
    removeFromIndex(t, elements.get(index));
    elements.remove(index);
    ++modCount;
    dirty = true;
    return t;
  } finally { leave(); }}}
  
  void removeChunk(int chunk) { synchronized(snapshot) { try { enter("removeChunk");
    assertMutable();
    if (!elements.removeElement(chunk))
      throw new InternalSmyleError(
        "removeChunk: Chunk "+chunk+" isn't part of table ("+"size="+elements.size()+")");
    ++modCount;
    dirty = true;
    if (!indexes.isEmpty())
      removeFromIndex(loadElementChunk(chunk), chunk);
  } finally { leave(); }}}
  
  void collectChunks(BitSet whiteList) { synchronized(snapshot) { try { enter("collectChunks");
    whiteList.set(schemaChunk.index);
    for (int i = 0; i < elements.size(); i++)
      whiteList.set(elements.get(i));
    for (int i = 0; i < indexes.size(); i++)
      indexes.get(i).collectChunks(whiteList);
    //if (debug) System.out.println("Adding "+elements.size()+" elements to whitelist (total now "+whiteList.size()+")");
  } finally { leave(); }}}
  
  void saveSchema() {
    if (type == null || type.typeDef == null) { 
      schemaChunk = ChunkManager.NULLCHUNK; 
    } else {
      Buffer b = new Buffer();
      type.writeRecursive(b);
      schemaChunk = snapshot.newChunkManager.createChunk(b);
    }
    dirty = true;
  }
  
  public TypeDef getSchema() { synchronized(snapshot) { try { enter("getSchema");
    return type.typeDef;
  } finally { leave(); }}}
  
  public IntVector getUniqueValues(Function<T,Integer> function) { synchronized(snapshot) { try { enter("getUniqueValues");
    IntVector v = new IntVector();
    for (int i = 0; i < elements.size(); i++) {
      int value = function.of(loadElement(i)).intValue();
      if (!v.contains(value)) v.add(value);
    }
    return v;
  } finally { leave(); }}}
  
  public <A> Map<A,Integer> getUniqueValueCounts(Function<T,A> function) {
    return getUniqueValueCounts(function, null);
  }
  
  public <A> Map<A,Integer> getUniqueValueCounts(Function<T,A> function,
    Filter<T> filter) { synchronized(snapshot) { try { enter("getUniqueValueCounts");
    HashMap<A,Integer> map = new HashMap<A,Integer>();
    
    for (Iterator<T> i = iterator(filter); i.hasNext(); ) {
      A value = function.of(i.next());
      Integer c = map.get(value);
      if (c == null) c = new Integer(1); else c = new Integer(c.intValue()+1);
      map.put(value, c);
    }
    return map;
  } finally { leave(); }}}
  
  public int indexOf(T t) { synchronized(snapshot) { try { enter("indexOf(T)");
    return indexOf(elementToFilter(t));
  } finally { leave(); }}}
  
  public int indexOf(T t, int startIndex) { synchronized(snapshot) { try { enter("indexOf(T,int)");
    for (int i = startIndex; i < elements.size(); i++) {
      if (t.equals(loadElement(i)))
        return i;
    }
    return -1;
  } finally { leave(); }}}
  
  public int lastIndexOf(T t) { synchronized(snapshot) { try { enter("lastIndexOf(T)");
    return lastIndexOf(t, elements.size()-1);
  } finally { leave(); }}}
  
  public int lastIndexOf(T t, int startIndex) { synchronized(snapshot) { try { enter("lastIndexOf(T,int)");
    for (int i = startIndex; i >= 0; i--) {
      if (t.equals(loadElement(i)))
        return i;
    }
    return -1;
  } finally { leave(); }}}
  
  public T set(int index, T t) { synchronized(snapshot) { try { enter("set(int,T)");
    assertMutable();
    T old = loadElement(index);
    removeFromIndex(old, elements.get(index));
    saveElement(t, index);
    return old;
  } finally { leave(); }}}
  
  // TODO: test all following methods
  
  public Object[] toArray(Object[] array) { synchronized(snapshot) { try { enter("toArray");
    for (int i = 0; i < elements.size(); i++)
      array[i] = loadElement(i);
    for (int i = elements.size(); i < array.length; i++)
      array[i] = null;
    return array;
  } finally { leave(); }}}

  public Object[] toArray() { synchronized(snapshot) { try { enter("toArray");
    Object[] array = new Object[size()];
    for (int i = 0; i < elements.size(); i++)
      array[i] = loadElement(i);
    return array;
  } finally { leave(); }}}
  
  public boolean containsAll(Collection<T> c) { synchronized(snapshot) { try { enter("containsAll");
    for (Iterator<T> i = c.iterator(); i.hasNext(); )
      if (!contains(i.next()))
        return false;
    return true;
  } finally { leave(); }}}
  
  public boolean removeAll(Collection<T> c) { synchronized(snapshot) { try { enter("removeAll(Collection)");
    assertMutable();
    boolean changed = false;
    for (int i = 0; i < elements.size(); i++) {
      if (c.contains(loadElement(i))) {
        remove(i--);
        changed = true;
      }
    }
    return changed;
  } finally { leave(); }}}
  
  public boolean retainAll(Collection<T> c) { synchronized(snapshot) { try { enter("retainAll");
    assertMutable();
    boolean changed = false;
    for (int i = 0; i < elements.size(); i++) {
      if (!c.contains(loadElement(i))) {
        remove(i--);
        changed = true;
      }
    }
    return changed;
  } finally { leave(); }}}
  
  /*public void removeRange(int startIndex, int endIndex) { synchronized(snapshot) {
    assertMutable();
    ++modCount;
    elements.removeRange(startIndex, endIndex);
  }}*/
  
  public ListIterator<T> listIterator() { synchronized(snapshot) { try { enter("listIterator");
    throw new RuntimeException("TODO");
  } finally { leave(); }}}
  
  public ListIterator<T> listIterator(int index) { synchronized(snapshot) { try { enter("listIterator");
    throw new RuntimeException("TODO");
  } finally { leave(); }}}
  
  public List<T> subList(int startIndex, int endIndex) { synchronized(snapshot) { try { enter("subList");
    throw new RuntimeException("TODO");
  } finally { leave(); }}}
  
  private void addToIndex(T t, ChunkRef chunk) {
    for (int i = 0; i < indexes.size(); i++)
      indexes.get(i).add(t, chunk);
  }
  
  private void removeFromIndex(T t, int chunk) {
    for (int i = 0; i < indexes.size(); i++)
      indexes.get(i).remove(t, new ChunkRef(chunk));
  }
  
  public IndexAdvisor<Pair<IndexProfile,Function>> getIndexAdvisor() { synchronized(snapshot) { try { enter("getIndexAdvisor");
    return indexAdvisor;
  } finally { leave(); }}}
  
  public int numIndexes() {
    return indexes.size();
  }
  
  public Index<T> getIndex(int i) {
    return indexes.get(i);
  }
  
  boolean matches(Filter<T> filter, T t) { synchronized(snapshot) { try { enter("matches");
    if (filter.matches(t))
      return true;
    else {
      if (indexAdvisor != null) {
        IndexProfile profile = null;
        for (int i = 0; i < filter._numClauses(); i++) {
          Filter<T>.Clause clause = filter._getClause(i);
          if (clause.matches(t))
            // If clause matches, it wouldn't help to index it
            // (since the whole filter doesn't match)
            continue;
            
          if (isIndexable(clause.getFunction())) {
            if (profile == null) // TODO: cache profile
              profile = QueryOptimizer.createProfile(filter);
            indexAdvisor.queryPerformed(new Pair<IndexProfile,Function>[] { 
              new Pair<IndexProfile,Function>(profile, clause.getFunction()) }, 1);
          }
        }
      }
      return false;
    }
  } finally { leave(); }}}
  
  void buildIndexIfAdvised() {
    if (indexAdvisor == null || !snapshot.mutable || !typed()) return;
    Pair<IndexProfile,Function> pair = indexAdvisor.fieldToIndex();
    if (pair == null) return;
    IndexProfile profile = pair.a();
    Function f = pair.b();
    
    if (debug) System.out.println("field to index: "+profile+" "+f+" (advisor: "+indexAdvisor+")");
    
    int bestScore1 = 0, bestScore2 = 0;
    Index<T> bestIndex = null;
    
    for (int i = 0; i < indexes.size(); i++) {
      Index<T> idx = indexes.get(i);
      if (idx.fields.contains(f)) continue;
      int score1 = QueryOptimizer.indexScore(profile, idx.fields);
      ArrayList<Function> fieldsPlusF = new ArrayList<Function>(idx.fields);
      fieldsPlusF.add(f);
      int score2 = QueryOptimizer.indexScore(profile, fieldsPlusF);
      if (debug) System.out.println("score1="+score1+", score2="+score2);
      if (score2 > bestScore2) {
        bestScore2 = score2;
        bestScore1 = score1;
        bestIndex = idx;
      }
    }
    
    ArrayList<Function> newFields;
    
    if (bestIndex != null) {
      if (bestScore2 == bestScore1) return; // no improvement possible
      newFields = new ArrayList<Function>(bestIndex.fields);
    } else {
      newFields = new ArrayList<Function>();
    }
    
    newFields.add(f);
    if (alreadyIndexed(newFields)) return;
    
    long startTime = System.currentTimeMillis();
    if (bestIndex == null)
      indexes.add(new Index(this, newFields));
    else
      bestIndex.addField(f);
    long endTime = System.currentTimeMillis();
    log("Indexed "+profile+" "+f+" (elements: "+elements.size()+"), number of indexes now: "+indexes.size()
      +"; "+(endTime-startTime)+" ms");
  }
  
  public void addIndex(ArrayList<Function> fields) {
    indexes.add(new Index(this, fields));
  }
  
  private boolean alreadyIndexed(ArrayList<Function> fields) {
    for (int i = 0; i < indexes.size(); i++)
      if (startsWith(indexes.get(i).fields, fields))
        return true;
    return false;
  }
  
  private static boolean startsWith(List<Function> a, List<Function> b) {
    if (a.size() < b.size()) return false;
    for (int i = 0; i < b.size(); i++)
      if (!a.get(i).equals(b.get(i)))
        return false;
    return true;
  }
  
  public void setAutoIncrementField(Field<T,Integer> field) { synchronized(snapshot) { try { enter("setAutoIncrementField");
    if (autoincField != field) {
      autoincField = field;
      
      // set counter to max value
      
      Filter<T> filter = new Filter<T>();
      filter._orderBy(field);
      filter._setReverse(true);
      Iterator<T> it = iterator(filter);
      if (it.hasNext())
        autoincCounter = Math.max(0, field.of(it.next()).intValue());
      else
        autoincCounter = 0;
        
      // assign an id to all records with field value 0
      
      for (int i = 0; i < size(); i++) {
        T t = get(i);
        if (field.of(t).intValue() == 0) {
          field.set(t, new Integer(++autoincCounter));
          set(i, t);
        }
      }
    }
  } finally { leave(); }}}
  
  public boolean typed() { synchronized(snapshot) { try { enter("typed");
    return demarshaller != null;
  } finally { leave(); }}}
  
  protected StructInfo<T> loadType() {
    if (schemaChunk.index != 0) {
      Buffer b = chunkManager.readChunk(schemaChunk);
      return snapshot.store.version < 292 ? new StructInfo(b) : StructInfo.readRecursive(b);
    } else
      return null;
  }
  
  ChunkRef getElementChunkRef(int index) { synchronized(snapshot) { try { enter("getElementChunkRef");
    return new ChunkRef(elements.get(index));
  } finally { leave(); }}}
  
  private Filter<T> elementToFilter(T t) {
    DynFilter<T> filter = new DynFilter<T>();
    for (int i = 0; i < type.typeDef.contents.size(); i++) {
      TypeComponent tc = type.typeDef.contents.get(i);
      Field f = fmd.getField(i);
      filter.functionEquals(f, f.of(t));
    }
    return filter;
  }

  /** only call this before an index is created! */
  public void setMaxKeyLength(int l) { 
    maxKeyLength = l; 
    maxKey = UniversalKey.max(l);
  }

  private void log(String msg) {
    snapshot.store.log(msg);
  }

  public Object[] untypedElementAt(int i) { synchronized(snapshot) { try { enter("untypedElementAt");
    StructInfo<Record> info = new StructInfo<Record>(type.typeDef, null, Record.class, null);
    int chunk = elements.get(i);
    return constructCustomDemarshaller(type, info,
      new HashMap<Pair<StructInfo,StructInfo>,FieldBasedDemarshaller>()).read(chunkManager.readChunk(chunk)).getArray();
  } finally { leave(); }}}
  
  private void enter(String function) {
    if (profiling) {
      if (snapshot.recCount++ == 0) {
        snapshot.workingSince = System.currentTimeMillis();
        snapshot.currentThread = Thread.currentThread();
        snapshot.currentFunctions = new String[20];
      }
      snapshot.currentFunctions[snapshot.recCount-1] = function;
    }
  }
  
  private void leave() {
    if (profiling) {
      snapshot.currentFunctions[snapshot.recCount-1] = null;
      if (--snapshot.recCount == 0) {
        snapshot.currentThread = null;
        snapshot.currentFunctions = null;
      }
    }
  }
  
  public void clearIndexes() {
    for (int i = 0; i < indexes.size(); i++)
      indexes.get(i).clear();
  }
  
  public String getName() {
    return snapshot.tableNames.get(id);
  }
}

