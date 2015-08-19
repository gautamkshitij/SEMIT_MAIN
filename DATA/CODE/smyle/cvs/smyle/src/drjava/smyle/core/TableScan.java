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

class TableScan<T extends Struct<T>> implements TableIterator<T> {
  TableImpl<T> table;
  Filter<T> filter;
  int expectedModCount;
  int i = -1;
  
  TableScan(TableImpl<T> table, Filter<T> filter) {
    this.table = table;
    expectedModCount = table.modCount;
    this.filter = filter;
    findNext();
  }
  
  private void checkForComodification() {
    if (table.modCount != expectedModCount)
      throw new ConcurrentModificationException();
  }
  
  public boolean hasNext() {
    checkForComodification();
    return i < table.size();
  }
  
  public T next() {
    checkForComodification();
    T result = table.loadElement(i);
    findNext();
    return result;
  }
  
  public ChunkRef nextChunk() {
    checkForComodification();
    ChunkRef result = table.getElementChunkRef(i);
    findNext();
    return result;
  }
  
  public void remove() {
    checkForComodification();
    ++expectedModCount;
    table.remove(i-1);
    i -= 2;
    //System.out.println("i now: "+i+" table size now: "+table.size());
    findNext();
  }

  void findNext() {
    while (++i < table.size()) {
      if (filter == null || table.matches(filter, table.loadElement(i)))
        return;
    }
  }
}

