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
import java.util.*;
import junit.framework.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;
import drjava.smyle.testtypes.*;
import drjava.util.*;
import org.artsProject.util.*;

public class GeneratedCodeTest extends TestCase {
  public GeneratedCodeTest(String name) { super(name); }

  DataTypes dt = new DataTypes(true, 1, 2, "test", new JustName("john"))
    .bytesAdd((byte) 17)
    .longsAdd(3)
    .stringsAdd("test")
    .structsAdd(new JustName("anne"));
    
  ByteVector bytes;
  IntVector longs;
  ArrayList<String> strings;
  ArrayList<JustName> structs;
    
  public void setUp() {
    bytes = new ByteVector();
    longs = new IntVector();
    strings = new ArrayList<String>();
    structs = new ArrayList<JustName>();
    bytes.add((byte) 17);
    longs.add(3);
    strings.add("test");
    structs.add(new JustName("anne"));
  }
    
  public void testFFields() {
    assertEquals(ComparableBoolean.TRUE, DataTypes.f_b.of(dt));
    assertEquals(new Integer(1), DataTypes.f_l.of(dt));
    assertEquals(new Long(2), DataTypes.f_ll.of(dt));
    assertEquals("test", DataTypes.f_s.of(dt));
    assertEquals(new JustName("john"), DataTypes.f_strct.of(dt));
    assertEquals(bytes, DataTypes.f_bytes.of(dt));
    assertEquals(longs, DataTypes.f_longs.of(dt));
    assertEquals(strings, DataTypes.f_strings.of(dt));
    assertEquals(structs, DataTypes.f_structs.of(dt));
  }
  
  public void testNestedFFields() {
    NestedDataTypes nested = new NestedDataTypes(dt);
    assertEquals(ComparableBoolean.TRUE, NestedDataTypes.f_nested_b.of(nested));
    assertEquals(new Integer(1), NestedDataTypes.f_nested_l.of(nested));
    assertEquals(new Long(2), NestedDataTypes.f_nested_ll.of(nested));
    assertEquals("test", NestedDataTypes.f_nested_s.of(nested));
    assertEquals(new JustName("john"), NestedDataTypes.f_nested_strct.of(nested));
    assertEquals(bytes, NestedDataTypes.f_nested_bytes.of(nested));
    assertEquals(longs, NestedDataTypes.f_nested_longs.of(nested));
    assertEquals(strings, NestedDataTypes.f_nested_strings.of(nested));
    assertEquals(structs, NestedDataTypes.f_nested_structs.of(nested));
  }

  public void testCopyConstructors() {
    assertEquals(dt, new DataTypes(dt));
  }

  /** also tests autoincrement */
  public void testStore() {
    Store store1 = Smyle.createEmptyStore("temp/store");
    Snapshot snapshot = store1.mutableSnapshot();
    snapshot.getTable("autoincs", AutoInc.getTypeInfo()).add(new AutoInc("green", 0));
    snapshot.commit();
    store1.close();

    MyStore store = MyStore.open("temp/store");
    Table<AutoInc> table = store.autoincs(store.snapshot());
    assertEquals(1, table.get(0).id);
    store.close();

    store = MyStore.create("temp/store");
    assertEquals(0, store.autoincs(store.snapshot()).size());
    store.close();
    
    MyStore.open(new File("temp/store")).close();
    MyStore.create(new File("temp/store")).close();
    
    store = MyStore.openReadOnly("temp/store");
    assertEquals(0, store.autoincs(store.snapshot()).size());
    try {
      store.mutableSnapshot();
      fail("No exception");
    } catch (ReadOnlyException e) {
      // ok
    }
    store.close();
  }
  
  public void testRecursiveDataTypes() {
    assertNotNull(Recursive.getTypeInfo().fieldTypes[0]);
    Recursive r = new Recursive(), r2 = new Recursive();
    r.recAdd(r2);
    assertEquals(r, Recursive.fromHex(r.toHex()));
  }
}
