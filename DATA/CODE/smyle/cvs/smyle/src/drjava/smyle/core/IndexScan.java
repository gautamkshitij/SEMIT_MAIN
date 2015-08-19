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
import drjava.smyle.*;
import drjava.smyle.meta.*;

/** a TableIterator (enhanced iterator) that scans through an index.
    performs filtering and local sorting (within an index leaf) */
class IndexScan<T extends Struct<T>> implements TableIterator<T> {
  TableImpl<T> table;
  Filter<T> filter;
  Index<T> index;
  boolean reversed;
  KeySet<UniversalKey> keySet;
  MapIterator<UniversalKey,ChunkRefs> it;
  TableIterator<T> it2;
  int expectedModCount, elementsReturned;
  T t;
  ChunkRef prevChunk, chunk;
  UniversalKey currentKey;
  
  // for debugging
  int itCount, it2Count;
  
  IndexScan(TableImpl<T> table, Filter<T> filter, Index<T> index) {
    init(table, filter, index, null);
  }
  
  IndexScan(TableImpl<T> table, Filter<T> filter, Index<T> index, KeySet<UniversalKey> keySet) {
    init(table, filter, index, keySet);
  }
  
  private void init(TableImpl<T> table, Filter<T> filter, Index<T> index, KeySet<UniversalKey> keySet) {
    this.table = table;
    this.filter = filter;
    this.index = index;
    this.keySet = keySet;
    this.reversed = filter.isReversed();
    
    init();
  }
  
  private void init() {
    expectedModCount = table.modCount;
    
    if (keySet != null)
      it = index.tree.iterate(null, null, keySet, reversed);
    else
      it = index.tree.iterate(reversed);
    if (TableImpl.debug) itCount = 0;
      
    findNext();
  }
  
  private void checkForComodification() {
    if (table.modCount != expectedModCount)
      throw new ConcurrentModificationException();
  }
  
  public boolean hasNext() {
    checkForComodification();
    return chunk != null;
  }
  
  public T next() {
    checkForComodification();
    T result = t != null ? t : table.loadElement(chunk);
    findNext();
    return result;
  }
  
  public ChunkRef nextChunk() {
    checkForComodification();
    findNext();
    return prevChunk;
  }
  
  public void remove() {
    checkForComodification();
    table.removeChunk(prevChunk.index);
    int n = elementsReturned;
    if (TableImpl.debug) System.out.println("IndexScan.remove(), n="+n);
    restart();
    while (--n > 1)
      findNext();
  }
  
  private void restart() {
    elementsReturned = 0;
    chunk = null;
    it2 = null;
    init();
  }

  void findNext() {
    ++elementsReturned;
    if (TableImpl.debug && elementsReturned > 200000)
      throw new InternalSmyleError("IndexScan.findNext: Too many elements!");

    prevChunk = chunk;
    while (true) {
      if (it2 == null) {
        if (it == null || !it.hasNext()) {
          chunk = null;
          t = null;
          return;
        }

        if (TableImpl.debug && ++itCount > 20000)
          throw new InternalSmyleError("IndexScan.findNext: Too many tree leafs");
        ArrayList<ChunkRef> v = it.next().list;
        currentKey = it.getKey();
        if (reversed) {
          v = new ArrayList<ChunkRef>(v);
          Collections.reverse(v);
        }
        
        final Iterator<ChunkRef> i = v.iterator();
        it2 = new ChunklistScan<T>(table, i);
        it2Count = 0;
        
        if (v.size() > 1 && !filter._getOrder().isEmpty())
          it2 = new SortedIterator<T>(table, it2, filter);
      }
      
      while (it2.hasNext()) {
        if (TableImpl.debug && ++it2Count > 5000)
          throw new InternalSmyleError("IndexScan.findNext: Too many elements returned by "+it2);
        chunk = it2.nextChunk();
        if (filter != null) {
          t = table.loadElement(chunk);
          if (table.matches(filter, t))
            return;
        } else {
          t = null;
          return;
        }
      }
      
      it2 = null;
    }
  }

  /*static volatile int globalId;
  TableIterator<T> sortByIndexField(int idx) {
    int id = globalId++;
    System.out.println("sortByIndexField "+id+": starting");
    ArrayList<ChunkRef> l = new ArrayList<ChunkRef>();
    final HashMap<ChunkRef,UniversalKey> keys = new HashMap<ChunkRef,UniversalKey>();
    int dims = table.indexedFields.size();

    while (chunk != null) {
      l.add(chunk);
      if ((l.size() & 1023) == 1)
        System.out.println("sortByIndexField "+id+": "+l.size()+" elements found, last is: "
          +table.loadElementChunk(chunk.index));
      keys.put(chunk, currentKey.extractDimension(idx, dims));
      findNext();
    }

    System.out.println("sortByIndexField "+id+": sorting "+l.size()+" elements");
    Collections.sort(l, new Comparator<ChunkRef>() {
      public int compare(ChunkRef a, ChunkRef b) {
        //System.out.println(table.loadElement(a)+" <-> "+table.loadElement(b));
        //System.out.println(keys.get(a)+" <-> "+keys.get(b));
        return reversed
          ? keys.get(b).compareTo(keys.get(a))
          : keys.get(a).compareTo(keys.get(b));
      }
    });

    System.out.println("sortByIndexField "+id+": returning");
    Iterator<ChunkRef> i = l.iterator();
    ChunklistScan result = new ChunklistScan(table, l.iterator());
    return result;
  }*/
}

