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

package drjava.smyle;

import java.io.*;
import java.util.*;
import org.artsProject.mcop.*;
import drjava.smyle.meta.*;

/** A view into a store; similar to what other databases call a transaction */
public interface Snapshot {
  /** returns a reference to a table.
      If a table with this name doesn't exist, it is created.
      If it exists, the requested schema (typeInfo) is compared to the table's current
      schema. If the schemas are compatible, the table is automatically converted
      to the new schema.<p>
      Throws a ClosedSnapshotException if the snapshot has been closed.
      */
  public <T extends Struct<T>> Table<T> getTable(String name,
  StructInfo<T> typeInfo) throws ClosedSnapshotException;
  
  /** saves all changes made in this snapshot and closes it.
      Throws an ImmutableSnapshotException if the snapshot is immutable.<br>
      Throws a ClosedSnapshotException if the snapshot is already closed.
      */
  public void commit() throws ConcurrentWriteException, ImmutableSnapshotException, ClosedSnapshotException;
  
  /** saves all changes made in this snapshot, but doesn't close it.
      Throws a ClosedSnapshotException if the snapshot has been closed.
      */
  public void commitAndContinue() throws ConcurrentWriteException, ImmutableSnapshotException, ClosedSnapshotException;
  
  /** undoes all changes made in this snapshot and closes it.
      If called on a closed snapshot, it just does nothing.
      */
  public void forget();
  
  public SortedSet<String> getTableNames();
  
  public UntypedTable getUntypedTable(String name);
}
