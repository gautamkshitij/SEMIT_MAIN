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

import java.util.*;
import org.artsProject.mcop.*;
import drjava.smyle.meta.*;

public class Index<T extends Struct<T>> implements Marshallable {
  TableImpl<T> table;
  ArrayList<Function> fields;
  PersistentBTree<UniversalKey,ChunkRefs> tree;
  
  public Index(TableImpl<T> table, ArrayList<Function> fields) {
    this.table = table;
    this.fields = fields;
    buildTree();
  }
  
  public Index(TableImpl<T> table, Buffer b) {
    this.table = table;
    fields = new ArrayList<Function>();
    MCOP.readSeq(b, fields, table.fmd);
    tree = new PersistentBTree<UniversalKey,ChunkRefs>
      (TableImpl.INDEX_M, null, new UniversalKey.MarDemar(),
      new TypeMarDemar(ChunkRefs.DEMARSHALLER), table.snapshot.newChunkManager, new ChunkRef(b));
      // TODO: load on demand?
  }
  
  public BTree<UniversalKey,ChunkRefs> getTree() {
    return tree;
  }
  
  private void buildTree() {
    synchronized(table.snapshot) {
      tree = new PersistentBTree<UniversalKey,ChunkRefs>
        (TableImpl.INDEX_M, null, new UniversalKey.MarDemar(),
        new TypeMarDemar(ChunkRefs.DEMARSHALLER), table.snapshot.newChunkManager, null);
      for (int i = 0; i < table.elements.size(); i++) {
        T t = table.loadElement(i);
        UniversalKey key = keyForElement(t);
        ChunkRefs cr = tree.get(key);
        if (cr == null) cr = new ChunkRefs();
        cr.listAdd(new ChunkRef(table.elements.get(i)));
        tree.put(key, cr);
      }
    }
  }
  
  private UniversalKey keyForElement(T t) {
    UniversalKey[] keys = new UniversalKey[fields.size()];
    for (int j = 0; j < fields.size(); j++)
      keys[j] = table.cutKey(new UniversalKey(fields.get(j).of(t)));
    return UniversalKey.concatDimensions(keys);
  }
  
  public void addField(Function f) {
    fields.add(f);
    buildTree();
  }
  
  public UniversalKey makeKeyPrefix(Filter<T> filter) {
    ArrayList<Object> list = new ArrayList<Object>();
    for (int i = 0; i < fields.size(); i++) {
      Filter<T>.Clause c = findClause(filter, fields.get(i));
      if (c == null) break;
      list.add(table.cutKey(new UniversalKey(c.getValue())));
    }
    return UniversalKey.concatDimensions(list.toArray(new Object[list.size()]));
  }
  
  private Filter<T>.Clause findClause(Filter<T> filter, Function f) {
    // TODO: optimize
    for (int i = 0; i < filter._numClauses(); i++) {
      Filter<T>.Clause c = filter._getClause(i);
      if (f.equals(c.getFunction()))
        return c;
    }
    return null;
  }
  
  public void clear() {
    tree.clear();
  }
  
  public void marshal(Buffer b) {
    MCOP.writeSeq(b, fields);
    tree.flush().marshal(b);
  }
  
  public void add(T t, ChunkRef chunk) {
    UniversalKey key = keyForElement(t);
    ChunkRefs cr = tree.get(key);
    //if (debug) System.out.println("Adding to index: "+indexedFields.get(0).of(t)+", crs: "+cr);
    if (cr == null) cr = new ChunkRefs();
    cr.listAdd(chunk);
    tree.put(key, cr);
  }
  
  public void remove(T t, ChunkRef chunk) {
    UniversalKey key = keyForElement(t);
    ChunkRefs cr = tree.get(key);
    if (cr.list.size() == 1) {
      tree.remove(key);
    } else {
      cr.list.remove(chunk);
      tree.put(key, cr);
    }
  }
  
  public void collectChunks(BitSet whiteList) {
    tree.collectChunks(whiteList);
  }
}
