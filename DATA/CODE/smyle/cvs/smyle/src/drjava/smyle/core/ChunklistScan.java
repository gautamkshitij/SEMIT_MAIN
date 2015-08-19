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
import drjava.smyle.meta.*;

class ChunklistScan<T extends Struct<T>> implements TableIterator<T> {
  TableImpl<T> table;
  Iterator<ChunkRef> i;
  
  ChunklistScan(TableImpl<T> table, Iterator<ChunkRef> i) {
    this.table = table;
    this.i = i;
  }
  
  public boolean hasNext() { return i.hasNext(); }
  public T next()	{ return table.loadElement(i.next()); }
  public void remove()	{ throw new RuntimeException("not implemented"); }
  public ChunkRef nextChunk() { return i.next(); }
}

