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

package drjava.smyle.tests;

import java.io.*;
import junit.framework.*;
import java.util.*;
import drjava.util.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;
import drjava.smyle.meta.*;
import drjava.smyle.core.*;
import org.artsProject.util.*;

public class UntypedTableTest extends DiskStoreTestBase {
  UntypedTable ut;
  
  public UntypedTableTest(String name) {
    super(name);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    table.add(john);
  }
  
  public void testTableNames() throws Exception {
    SortedSet<String> names = snapshot.getTableNames();
    assertEquals(1, names.size());
    assertTrue(names.contains("person"));
  }
  
  void loadUntypedTable() throws Exception {
    commitAndGetNewSnapshot();
    ut = snapshot.getUntypedTable("person");
  }
  
  public void testUntypedTable() throws Exception {
    loadUntypedTable();

    assertEquals(Person.getTypeInfo().typeDef, ut.getSchema());
    assertEquals(1, ut.size());

    Object[] row = ut.untypedElementAt(0);
    assertEquals(2, row.length);
    assertEquals(john.name, row[0]);
    assertEquals(new Integer(john.age), row[1]);
  }
  
  /*public void testIndex() throws Exception {
    TableImpl tableImpl = (TableImpl)(Table) table;
    tableImpl.indexField(Person.f_age);
    
    // make sure getTable isn't called before getUntypedTable
    snapshot.commit();
    snapshot = store.snapshot();    
    ut = snapshot.getUntypedTable("person");
    
    assertEquals(Person.getTypeInfo().typeDef, ut.getSchema());
    assertEquals(Person.f_age.nr(), ((Field) ut.getIndexedField()).nr());
  }XX*/
}
