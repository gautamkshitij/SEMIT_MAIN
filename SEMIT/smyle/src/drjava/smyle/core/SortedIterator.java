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

final class SortedIterator<T extends Struct<T>> implements TableIterator<T> {
  TableImpl<T> table;
  Filter<T> filter;
  int expectedModCount;
  int i = 0;
  ArrayList<Entry> entries = new ArrayList<Entry>();
  
  class Entry implements Comparable {
    Comparable[] key;
    ChunkRef chunk;
    
    Entry(Comparable[] key, ChunkRef chunk) {
      this.key = key;
      this.chunk = chunk;
    }
    
    public int compareTo(Object o) {
      for (int i = 0; i < key.length; i++) {
        int c = key[i].compareTo(((Entry) o).key[i]);
        if (c != 0) {
          return filter.isReversed() ? -c : c;
        }
      }
      return 0;
    }
  }
  
  SortedIterator(TableImpl<T> table, TableIterator<T> iterator, Filter<T> filter) {
    this.table = table;
    expectedModCount = table.modCount;
    this.filter = filter;
    scan(iterator);
  }
  
  private void checkForComodification() {
    if (table.modCount != expectedModCount)
      throw new ConcurrentModificationException();
  }
  
  public boolean hasNext() {
    checkForComodification();
    return i < entries.size();
  }
  
  public T next() {
    checkForComodification();
    return table.loadElement(entries.get(i++).chunk);
  }
  
  public ChunkRef nextChunk() {
    checkForComodification();
    return entries.get(i++).chunk;
  }
  
  public void remove() {
    checkForComodification();
    ++expectedModCount;
    table.removeChunk(entries.get(--i).chunk.index);
  }

  void scan(TableIterator<T> iterator) {
    // gather keys and chunkrefs
    
    Function<T,Comparable> order = filter._getOrder().get(0);
    while (iterator.hasNext()) {
      ChunkRef cr = iterator.nextChunk();
      T t = table.loadElement(cr);
      /*if (filter != null && !filter.matches(t))
        continue;*/
      Comparable[] key = new Comparable[filter._getOrder().size()];
      for (int i = 0; i < key.length; i++)
        key[i] = filter._getOrder().get(i).of(t);
      entries.add(new Entry(key, cr));
    }
    
    // sort
    
    Collections.sort(entries);
  }
}

