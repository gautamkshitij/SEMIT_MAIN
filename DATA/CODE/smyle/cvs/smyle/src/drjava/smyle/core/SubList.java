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
import drjava.smyle.*;
import drjava.smyle.meta.*;

class SubList<T extends Struct<T>> extends AbstractList<T> {
  final TableImpl<T> table;
  final List<ChunkRef> chunkRefs;
  
  SubList(TableImpl<T> table, List<ChunkRef> chunkRefs) {
    this.table = table;
    this.chunkRefs = chunkRefs;
  }
    
  public int size() {
    return chunkRefs.size();
  }
  
  public T get(int index) {
    return table.loadElement(chunkRefs.get(index));
  }
  
  /*public T set(int index, T t) { synchronized(table.snapshot) {
    table.assertMutable();
    ChunkRef chunk = chunkRefs.get(index);
    int index2 = table.elements.indexOf(chunk);
    T old = table.loadElement(index2);
    table.removeFromIndex(old, chunk);
    table.saveElement(t, index2);
    chunkRefs.set(index, table.elements.get(index2));
    return old;
  }}*/
  
  public List<T> subList(int i, int j) {
    return new SubList<T>(table, chunkRefs.subList(i, j));
  }
}
