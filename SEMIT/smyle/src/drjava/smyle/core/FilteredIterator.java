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
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import org.artsProject.util.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;

final class FilteredIterator<T extends Struct<T>> implements TableIterator<T> {
  TableImpl<T> table;
  Filter<T> filter;
  TableIterator<T> iterator;
  ChunkRef chunk;
  T t;
  
  FilteredIterator(TableImpl<T> table, TableIterator<T> iterator, Filter<T> filter) {
    this.table = table;
    this.iterator = iterator;
    this.filter = filter;
    findNext();
  }
  
  public boolean hasNext() {
    return chunk != null;
  }
  
  public T next() {
    T t = this.t;
    findNext();
    return t;
  }
  
  public ChunkRef nextChunk() {
    ChunkRef chunk = this.chunk;
    findNext();
    return chunk;
  }
  
  public void remove() {
    throw new RuntimeException("not implemented");
  }

  void findNext() {
    while (iterator.hasNext()) {
      chunk = iterator.nextChunk();
      t = table.loadElement(chunk);
      if (filter.matches(t))
        return;
    }
    chunk = null;
    t = null;
  }
}

