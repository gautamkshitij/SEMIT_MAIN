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
import drjava.gjutil.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;
import drjava.smyle.core.*;
import drjava.smyle.core.indexing.*;
import drjava.smyle.meta.*;
import org.artsProject.mcop.core.*;
import org.artsProject.util.*;

public class IndexTest extends DiskStoreTestBase {
  public IndexTest(String name) {
    super(name);
  }
  
  TableImpl<Person> tableImpl;
  PersistentBTree<UniversalKey,ChunkRefs> index;
  IndexAdvisor<Pair<IndexProfile,Function>> indexAdvisor;
  int ageStats, nameStats;
  Index<Person> idx;
  IndexProfile profile;
  Person_filter filter;
  
  public void setUp() throws Exception {
    super.setUp();
    snapshot.commit();
    store.enableIndexing();
    getNewSnapshot();
    tableImpl = (TableImpl<Person>) table;
    ageStats = 0;
    filter = null;
  }
  
  void checkIndex() throws Exception {
    // convert tree to map
    
    Iterator<Map.Entry<UniversalKey,ChunkRefs>> iIndex
      = BTreeTestBase.treeToMap(index).entrySet().iterator();
      
    // walk through map and compare with table
      
    Iterator<Person> iTable = table.iterator();
    while (iIndex.hasNext()) {
      Map.Entry<UniversalKey,ChunkRefs> eI = iIndex.next();
      ArrayList<ChunkRef> v = eI.getValue().list;
      for (int i = 0; i < v.size(); i++) {
        assertTrue("table has next at "+i+" of "+v.size(), iTable.hasNext());
        Person eT = iTable.next();
        assertEquals(new UniversalKey(eT.age), eI.getKey());
        assertEquals(eT, tableImpl.loadElement(v.get(i)));
      }
    }
    assertTrue(!iTable.hasNext());
  }
  
  /** builds an index that is inconsistent with the table contents
      (prerequisite to verify that certain methods use the index) */
  void buildInconsistentIndex() throws Exception {
    table.add(john);
    table.add(anne);
    table.add(jane);
    // john and jane have same age (test duplicates)
    /*List<Function> fields = Collections.singletonList((Function) Person.f_age);
    index = tableImpl.buildIndex(fields);
    
    // make table and index inconsistent to verify that
    // queries go through the index
    table.clear();
    tableImpl.setIndex(fields, index);XX*/
  }
  
  public void testOrderByOverCutIndex() throws Exception {
    tableImpl.setMaxKeyLength(4);
    indexName();
    Person john1 = new Person("John1", 23);
    Person john2 = new Person("John2", 23);
    table.add(john2);
    table.add(john1);

    TestUtil.checkIterator(
      tableImpl.iterator(new Person_filter().orderByName()),
      new Person[] { anne, john, john1, john2 });
  }
  
  public void testOrderByPlusFilter() throws Exception {
    buildInconsistentIndex();
    
    TestUtil.checkIterator(
      tableImpl.iterator(new Person_filter().ageEquals(23)
        .orderByName().reverse()),
      new Person[] {john, jane});
  }
  
  public void testIndexedPlusOtherField() throws Exception {
    buildInconsistentIndex();
    
    // iterate
    
    TestUtil.checkIterator(
      tableImpl.iterator(new Person_filter()
        .nameEquals("John").ageEquals(23)),
      new Person[] {john});
    TestUtil.checkIterator(
      tableImpl.iterator(new Person_filter()
        .ageEquals(23).nameEquals("John")),
      new Person[] {john});
    TestUtil.checkIterator(
      tableImpl.iterator(new Person_filter()
        .ageEquals(21).nameEquals("John")),
      new Person[] {});
  }
  
  public void testContainsOverIndex() throws Exception {
    buildInconsistentIndex();
    
    assertTrue(table.contains(new Person_filter().ageEquals(23)));
    assertTrue(!table.contains(new Person_filter().ageEquals(99)));
  }
  
  /*public void testIndexOfOverIndex() throws Exception {
    indexAge();
    
    assertEquals(0, table.indexOf(new Person_filter().ageEquals(23)));
    assertEquals(1, table.indexOf(new Person_filter().ageEquals(21)));
    assertEquals(-1, table.indexOf(new Person_filter().ageEquals(99)));
  }*/
  
  public void testIndexOfObjectOverIndex() throws Exception {
    indexAge();
    
    assertEquals( 0, table.indexOf(john));
    assertEquals( 1, table.indexOf(anne));
    assertEquals(-1, table.indexOf(jane));
  }
  
  public void testRemoveAllOverIndex() throws Exception {
    indexAge();
    tableImpl.removeAll(filter = new Person_filter().ageEquals(95));
    TestUtil.checkIterator(table.iterator(), new Person[] {john, anne});
    tableImpl.removeAll(new Person_filter().ageEquals(23));
    TestUtil.checkIterator(table.iterator(), new Person[] {anne});
    
    // indirect way to check that index was used
    ageStats = 0; // because filter changed
    assertAgeStats(0);
  }
  
  /*public void testRemoveAllOverCutIndex() throws Exception {
    tableImpl.setMaxKeyLength(4);
    indexName();
    tableImpl.add(new Person("Horst", 45));
    tableImpl.removeAll(new Person_filter().nameEquals("Horst"));
    TestUtil.checkIterator(table.iterator(), new Person[] {john, anne});
  }*/

  void assertAgeStats(int diff) throws Exception {
    indexAdvisor = tableImpl.getIndexAdvisor();
    IndexProfile profile = QueryOptimizer.createProfile(filter);
    Pair<IndexProfile,Function> key = new Pair<IndexProfile,Function>(profile, Person.f_age);
    assertEquals("Looking for "+key+" in "+indexAdvisor, ageStats += diff, indexAdvisor.getStats(key));
  }
  
  void assertNameStats(int diff) throws Exception {
    //assertEquals(nameStats += diff, indexAdvisor.getStats(Person.f_name));
  }
  
  void fillTableAndPerformQuery() throws Exception {
    table.add(john);
    table.add(anne);
    table.count(filter = new Person_filter().nameEquals("Anne").ageEquals(21));
    assertAgeStats(1);  
  }
  
  public void testTableInformsIndexAdvisor() throws Exception {
    indexAdvisor = tableImpl.getIndexAdvisor();
    fillTableAndPerformQuery();
    table.contains(filter = new Person_filter().ageEquals(9));
    ageStats = 0; // because filter changed
    assertAgeStats(2);
    table.iterator(filter = new Person_filter().orderByAge());
    ageStats = 0; // because filter changed
    assertAgeStats(2);
    table.removeAll(filter = new Person_filter().ageEquals(21));
    assertAgeStats(1);
    
    //assertEquals(1, indexAdvisor.getStats(Person.f_name));
  }
  
  /*public void testAdvisorResetOnSchemaChange() throws Exception {
    indexAdvisor = tableImpl.getIndexAdvisor();
    fillTableAndPerformQuery();
    
    snapshot.forget();
    snapshot = store.mutableSnapshot();
    Table<ExtendedPerson> table = snapshot.getTable("person", ExtendedPerson.getTypeInfo());
    tableImpl = (TableImpl) table;
    indexAdvisor = tableImpl.getIndexAdvisor();
    assertAgeStats(0);
  }*/
  
  public void testAdvisorPreserved() throws Exception {
    indexAdvisor = tableImpl.getIndexAdvisor();
    fillTableAndPerformQuery();
    
    snapshot.forget();
    getNewSnapshot();
    //XXassertAgeStats(0);
    indexAdvisor = tableImpl.getIndexAdvisor();
    //XXassertAgeStats(0);
  }
  
  void prepareIndexCreation() throws Exception {
    indexAdvisor = tableImpl.getIndexAdvisor();
    indexAdvisor.setInitialHurdle(1);
    table.add(john);
    table.add(anne);
  }
  
  void indexAge() throws Exception {
    prepareIndexCreation();
    
    // iterate to trigger index creation
    Iterator<Person> i = tableImpl.iterator(filter = new Person_filter().orderByAge());
    TestUtil.checkIterator(i, new Person[] {anne, john});
    assertAgeStats(2);
  }
  
  void indexName() throws Exception {
    prepareIndexCreation();
    
    // iterate to trigger index creation
    Iterator<Person> i = tableImpl.iterator(filter = new Person_filter().orderByName());
    TestUtil.checkIterator(i, new Person[] {anne, john});
    assertNameStats(2);
  }
  
  void assertAgeIndexCreated() throws Exception {
    assertEquals(1, tableImpl.numIndexes());
    //XXassertEquals(Person.f_age, tableImpl.getIndexedField());
    Iterator<Person> i = tableImpl.iterator(new Person_filter().orderByAge());
    TestUtil.checkIterator(i, new Person[] {anne, john});
  }
  
  void assertNameIndexCreated() throws Exception {
    assertEquals(1, tableImpl.numIndexes());
    //XXassertEquals(Person.f_name, tableImpl.getIndexedField());
    Iterator<Person> i = tableImpl.iterator(new Person_filter().orderByName());
    TestUtil.checkIterator(i, new Person[] {anne, john});
  }
  
  public void testIntIndex() throws Exception {
    indexAge();
    assertAgeIndexCreated();    
  }
  
  public void testStringIndex() throws Exception {
    indexName();
    assertNameIndexCreated();

    table.add(new Person(null, 0));
    assertTrue(table.contains(new Person_filter().nameEquals(null)));
  }
  
  public void testCutIndex() throws Exception {
    tableImpl.setMaxKeyLength(4);
    indexName();
    assertNameIndexCreated();
  }
  
  public void testLongIndex() throws Exception {
    Table<DataTypes> table = snapshot.getTable("datatypes", DataTypes.getTypeInfo());
    TableImpl tableImpl = (TableImpl)(Table) table;
    tableImpl.getIndexAdvisor().setInitialHurdle(1);
    
    DataTypes r1 = new DataTypes(), r2 = new DataTypes();
    r1.strct = new JustName();
    r2.strct = new JustName();
    r1.ll = 12345678901234L;
    r2.ll = -12345678901234L;
    table.add(r1);
    table.add(r2);
    
    // iterate to trigger indexing
    TestUtil.checkIterator(table.iterator(new DataTypes_filter().orderByLl()),
      new DataTypes[] {r2,r1});
    
    // assert index was created
    //XXassertEquals(DataTypes.f_ll, tableImpl.getIndexedField());
    TestUtil.checkIterator(table.iterator(new DataTypes_filter().orderByLl()),
      new DataTypes[] {r2,r1});
  }
  
  /*public void testIndexMaintenance() throws Exception {
    indexAge();
    index = tableImpl.getIndex();
    indexAdvisor.freeze();
    
    table.clear();
    checkIndex();
    
    table.add(john);
    table.add(0, anne);
    checkIndex();
    
    table.set(1, jane);
    checkIndex();
    
    table.remove(john);
    checkIndex();
    
    table.add(john);
    Iterator<Person> i = table.iterator();
    i.next();
    i.remove();
    checkIndex();
    
    table.add(john);
    // ordered by non-indexed field (SortedTableScan)
    i = table.iterator(new Person_filter().orderByName());
    i.next();
    i.remove();
    checkIndex();
  }XX*/
  
  /*public void testIndexScanRemove() throws Exception {
    indexAge();
    index = tableImpl.getIndex();
    indexAdvisor.freeze();
    
    // ordered by indexed field (IndexScan)
    Iterator<Person> i = table.iterator(new Person_filter().orderByAge());
    assertEquals(anne, i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals(john, i.next());
    assertTrue(!i.hasNext());
    assertEquals(john, table.get(0));
    assertEquals(1, table.size());
    checkIndex();
  }XX*/
  
  /*public void testIndexScanRemoveSameValue() throws Exception {
    indexAge();
    index = tableImpl.getIndex();
    indexAdvisor.freeze();
    
    table.clear();
    for (int age = 15; age <= 25; age += 10)
      for (int n = 0; n < 5; n++)
        table.add(new Person(String.valueOf(n), age));
      
    Iterator<Person> i = null;
    for (int age = 15; age <= 25; age += 10) {
      i = table.iterator(new Person_filter().orderByAge());
      for (int n = 0; n < 5; n++) {
        assertTrue(i.hasNext());
        assertEquals(new Person(String.valueOf(n), age), i.next());
        i.remove();
      }
    }
    assertTrue(!i.hasNext());
    assertEquals(0, table.size());
    checkIndex();
  }XX*/
  
  /** asserts an index in an old snapshot is still accessible
      even it it was cleared in a more recent snapshot */
  public void testIndexPreserved() throws Exception {
    store.setWriteLatency(0);
    indexAge();
    commitAndGetNewSnapshot();
    Snapshot oldSnapshot = store.snapshot();
    Table<Person> oldTable = oldSnapshot.getTable("person", Person.getTypeInfo());
    assertEquals(1, tableImpl.numIndexes());
    ((TableImpl) (Table) table).clearIndexes();
    assertEquals(1, tableImpl.numIndexes());
    snapshot.commit();
    store.collectGarbage();
    tableImpl = (TableImpl) (Table) oldTable;
    assertAgeIndexCreated();
  }
  
  /** test "offline" table chunk collection code in SnapshotImpl */
  public void testIndexPreservedIfTableNotOpen() throws Exception {
    indexAge();
    snapshot.commit();
    rotate();
    tableImpl = (TableImpl) (Table) table;
    assertAgeIndexCreated();
  }
  
  public void testIndexBuiltOnRemoveAll() throws Exception {
    prepareIndexCreation();
    table.removeAll(new Person_filter().ageEquals(0));
    assertAgeIndexCreated();
  }
  
  void indexNameIC() throws Exception {
    prepareIndexCreation();
    
    // build subList to trigger index creation
    Iterator<Person> i = tableImpl.subList(
      new Person_filter().nameEqualsIgnoreCase("anne")).iterator();
    TestUtil.checkIterator(i, new Person[] {anne});
    
    // assert this isn't counted for the name field itself
    assertNameStats(0);
  }
  
  void assertNameICIndexCreated() throws Exception {
    assertEquals("Index created", 1, tableImpl.numIndexes());
    //assertEquals(Person.f_name, tableImpl.getIndexedField());
    Iterator<Person> i = tableImpl.iterator(
      new Person_filter().nameEqualsIgnoreCase("anne"));
    TestUtil.checkIterator(i, new Person[] {anne});
  }
  
  public void testICIndex() throws Exception {
    indexNameIC();
    assertNameICIndexCreated();
  }

  /*public void testMultidimIndex() throws Exception {
    indexAge();

    // iterate to trigger index creation
    for (int n = 0; n < 2; n++) {
      Iterator<Person> i = tableImpl.iterator(new Person_filter().orderByName());
      TestUtil.checkIterator(i, new Person[] {anne, john});
      assertNameStats(2);
    }

    assertNotNull(tableImpl.getIndex());
    assertEquals(Person.f_age,  tableImpl.getIndexedFields().get(0));
    assertEquals(Person.f_name, tableImpl.getIndexedFields().get(1));

    // query on both dimensions at once
    assertTrue(!tableImpl.contains(new Person_filter().nameEquals("John").ageEquals(21)));
    assertNameStats(0);
    assertAgeStats(0);

    // filter on one dimension, sort on other one
    table.add(jane);
    TestUtil.checkIterator(table.iterator(new Person_filter().ageEquals(23).orderByName()),
      new Person[] {jane, john});
      
    // sort on one of the two dimensions
    TestUtil.checkIterator(table.iterator(new Person_filter().orderByName().reverse()),
      new Person[] {john, jane, anne});
  }XX*/

  public void testMultidimStress() throws Exception {
    int n = 50;
    tableImpl.getIndexAdvisor().setInitialHurdle(1);

    for (int i = 0; i < n; i++) {
      table.add(new Person(String.valueOf(i), i));
    }

    for (int i = 0; i < n; i++) {
      table.removeAll(i % 2 == 0 ? new Person_filter().ageEquals(i)
                                 : new Person_filter().nameEquals(String.valueOf(i)));
      if (table.size() != n-i-1)
        assertEquals("Table size", n-i-1, table.size());
    }
  }

  public void testOrderByTwoFieldsFirstIndexed() throws Exception {
    indexAge();
    
    table.clear();
    table.add(john);
    table.add(anne);
    table.add(jane);
 
    Iterator<Person> i = table.iterator(new Person_filter().orderByAge().orderByName());
    TestUtil.checkIterator(i, new Person[] {anne,jane,john});
  }
  
  public void testBooleanIndex() throws Exception {
    Table<VeryExtendedPerson> table = snapshot.getTable("ext", VeryExtendedPerson.getTypeInfo());
    TableImpl<VeryExtendedPerson> tableImpl = (TableImpl<VeryExtendedPerson>) table;
    indexAdvisor = tableImpl.getIndexAdvisor();
    indexAdvisor.setInitialHurdle(1);
    VeryExtendedPerson john = new VeryExtendedPerson().name("John").married(true),
      anne = new VeryExtendedPerson().name("Anne").married(false);
    table.add(john);
    table.add(anne);

    // iterate to trigger index creation
    Iterator<VeryExtendedPerson> i = tableImpl.iterator(new VeryExtendedPerson_filter().orderByMarried());
    TestUtil.checkIterator(i, new VeryExtendedPerson[] {anne, john});
    
    /*assertNotNull(tableImpl.getIndex());
    assertEquals(VeryExtendedPerson.f_married, tableImpl.getIndexedFields().get(0));XX*/
    
    // check index
    i = tableImpl.iterator(new VeryExtendedPerson_filter().orderByMarried());
    TestUtil.checkIterator(i, new VeryExtendedPerson[] {anne, john});
  }
  
  /*public void testIndexConstruction() {
    createNameIndex();
    checkIndex(new UniversalKey("John"));
  }
  
  public void testAddField() {
    createNameIndex();
    idx.addField((Function) Person.f_age);
    checkIndex(UniversalKey.concatDimensions(new java.lang.Object[] { "John", new Integer(john.age) }));
  }
  
  void createNameIndex() {
    table = snapshot.getTable("people", Person.getTypeInfo());
    table.add(john);
    
    table.addIndex(
     idx = new Index<Person>((TableImpl<Person>) table, new Function[] { Person.f_name });
  }
  
  void checkIndex(UniversalKey expectedKey) {
    // check tree contents
    MapIterator<UniversalKey,ChunkRefs> i = idx.getTree().iterate();
    assertTrue(i.hasNext());
    i.next(); // don't check element...
    assertEquals(expectedKey, i.getKey());
    assertFalse(i.hasNext());
  }*/
}
