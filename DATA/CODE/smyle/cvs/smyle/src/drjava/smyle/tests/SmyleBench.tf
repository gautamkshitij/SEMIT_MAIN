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

// SmyleBench.tf
// Smyle benchmark script
// This is a Beanshell script that uses the drjava.tf framework

import drjava.smyle.core.*;
import drjava.smyle.testtypes.*;

void setUp() {
  // create store, snapshot, table
  super.store = new DiskStore(new MemoryDisk());
  super.snapshot = store.mutableSnapshot();
  super.table = snapshot.getTable("person", Person.getTypeInfo());
}

void benchAdd() {
  n = 100000;
  description("Add "+n+" records");
  person = new Person("John", 23);
  for (i = 0; i < n; i++)
    table.add(person);
}

