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
import drjava.smyle.core.Disk;
import drjava.smyle.core.FileSystemDisk;
import drjava.smyle.core.DiskStore;
import drjava.smyle.meta.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import drjava.gjutil.Factory;
import org.artsProject.util.*;

public class StoreTest extends StoreTestBase {
  public StoreTest(String name) {
    super(name);
  }
  
  public static Test suite(final Factory<Store> storeCreator, final Factory<Store> storeOpener, final boolean canRotate) {
    return new FactoryTestSuite(StoreTest.class, new TestFactory() {
      public TestCase createTest(String name) {
        StoreTestBase test = new StoreTest(name);
        test.init(storeCreator, storeOpener, canRotate);
        return test;
      }
    });
  }
  
  public static TestSuite suite() {
    return new AggregatedTestSuite("Store Tests",
      new Test[] {
        
        // memory
        
        /*suite(new Factory<Store>() {
          public Store produce() {
            return new MemoryStore();
          }
        }, false),*/
        
        // disk
        
        suite(new Factory<Store>() {
          public Store produce() {
            Disk disk = new FileSystemDisk(new File("temp/store"), false);
            disk.deleteEverything();
            return new DiskStore(disk);
          }
        }, new Factory<Store>() {
          public Store produce() {
            Disk disk = new FileSystemDisk(new File("temp/store"), false);
            return new DiskStore(disk);
          }
        }, true),
        
        // remote
        
        /*suite(new Factory<Store>() {
          public Store produce() {
            Dispatcher serverDispatcher = new Dispatcher(null);
            serverDispatcher.becomeGlobalDispatcher();
            Store store = new MemoryStore();
            Dispatcher clientDispatcher = new Dispatcher(null);
            clientDispatcher.becomeGlobalDispatcher();
            
            return Store.Factory.fromReference(store._getObjectReference());
          }
        }, true)*/
      }
    );
  }
  
  public void testEmptyTable() throws Exception {
    assertEquals(0, table.size());
    assertTrue(table.isEmpty());
    assertNull(table.first());
  }
  
  public void testStoreOneElement() throws Exception {
    table.add(john);
    assertEquals(1, table.size());
    assertTrue(!table.isEmpty());
    Person p = table.first();
    assertEquals(john, p);
  }
  
  public void testStoreOneElementCommit() throws Exception {
    table.add(john);
    commitAndGetNewSnapshot();
    assertEquals(1, table.size());
    Person p = table.first();
    assertEquals(john, p);
  }
  
  public void testRotateWithoutCommitEqualsRollback() throws Exception {
    if (!canRotate) return;
    table.add(john);
    rotate();
    assertEquals(0, table.size());
  }
  
  public void testStoreOneElementRotate() throws Exception {
    if (!canRotate) return;
    table.add(john);
    commitAndRotate();
    assertEquals(1, table.size());
    Person p = table.first();
    assertEquals(john, p);
  }
  
  public void testStoreTwoElements() throws Exception {
    table.add(john);
    table.add(anne);
    assertEquals(2, table.size());
    assertEquals(john, table.get(0));
    assertEquals(anne, table.get(1));
  }
  
  public void testSnapshotsAreIsolated() throws Exception {
    // forget mutable snapshot and acquire an immutable snapshot
    snapshot.forget();
    snapshot = store.snapshot();
    Table<Person> oldTable = snapshot.getTable("people", Person.getTypeInfo());
    
    getNewSnapshot();
    table.add(anne);
    snapshot.commit();
    
    assertEquals(0, oldTable.size());
    
    // assert immutable snapshots are always up-to-date
    // (ant not cached for too long)
    Snapshot newImmutable = store.snapshot();
    assertEquals(1, newImmutable.getTable("people", Person.getTypeInfo()).size());
  }
  
  public void testMutableImmutableMix() throws Exception {
    // forget an immutable snapshot while mutable snaphot is open
    Snapshot immutableSnapshot = store.snapshot();
    immutableSnapshot.getTable("people", Person.getTypeInfo());
    immutableSnapshot.forget();
    
    snapshot.commit();
    
    // do it again and check that it's usable
    immutableSnapshot = store.snapshot();
    System.out.println("store: " + store + ", snapshot: " + immutableSnapshot);
    immutableSnapshot.getTable("people", Person.getTypeInfo());
    immutableSnapshot.forget();
  }
  
  public void testInterleavedWrites() throws Exception {
    if (store.exclusiveWriteLocking()) return; // Test makes no sense with exclusive locking
    
    Snapshot oldSnapshot = snapshot;
    Table<Person> oldTable = table;
    oldTable.add(john);
    
    getNewSnapshot();
    table.add(anne);
    
    // commit snapshot 1, then snapshot 2
    
    oldSnapshot.commit();
    try {
      store.logTo(null);
      snapshot.commit();
      fail("Exception expected");
    } catch (ConcurrentWriteException e) {
      // ok
    }
  }
  
  public void testNestedWrites() throws Exception {
    if (store.exclusiveWriteLocking()) return; // Test makes no sense with exclusive locking
    
    Snapshot oldSnapshot = snapshot;
    Table<Person> oldTable = table;
    oldTable.add(john);
    
    getNewSnapshot();
    table.add(anne);
    
    // commit snapshot 2, then snapshot 1
    
    snapshot.commit();
    try {
      store.logTo(null);
      oldSnapshot.commit();
      fail("Exception expected");
    } catch (ConcurrentWriteException e) {
      // ok
    }
  }
  
  public void testFilter() throws Exception {
    table.add(john);
    table.add(anne);
    
    // catch-all filter, count()
    
    assertEquals(2, table.count(new Person_filter()));
    
    // find John by name, iterator()
    
    Person_filter filter = new Person_filter().nameEquals("John");
    Iterator<Person> i = table.iterator(filter);
    TestUtil.checkIterator(i, new Person[] {john});
    
    // test two-field filter, contains()
    
    filter = new Person_filter().nameEquals("Anne").ageEquals(29);
    assertTrue(!table.contains(filter));
    filter = new Person_filter().nameEquals("Anne").ageEquals(21);
    assertTrue(table.contains(filter));
    
    // test indexOf
    
    assertEquals(1, table.indexOf(new Person_filter().nameEquals("Anne")));
    assertEquals(0, table.indexOf(new Person_filter()));
    assertEquals(-1, table.indexOf(new Person_filter().ageEquals(99)));
    
    // test ...EqualsIgnoreCase
    
    filter = new Person_filter().nameEqualsIgnoreCase("aNne");
    assertEquals(1, table.count(filter));
    
    // test get
    assertEquals(anne, table.get(new Person_filter().nameEquals("Anne")));
    assertEquals(null, table.get(new Person_filter().nameEquals("Nico")));
    
    // test subList
    TestUtil.checkIterator(
      table.subList(new Person_filter().nameEquals("Anne")).iterator(),
      new Person[] {anne});
      
    // test null values
    assertEquals(null, table.get(new Person_filter().nameEquals(null)));
  }
  
  /* XXX - hm, do we actually want to allow null in records and queries?
  public void testNullInFilter() throws Exception {
    Person nullPerson = new Person(null, 0);
    
    table.add(john);
    table.add(nullPerson);
    
    // test null in filter, first()
    
    assertEquals(nullPerson, table.first(new Person_filter().nameEquals(null)));
  }*/
  
  public void testTwoTablesRotate() throws Exception {
    Table<Person> boys = snapshot.getTable("boys", Person.getTypeInfo());
    Table<Person> girls = snapshot.getTable("girls", Person.getTypeInfo());
    boys.add(john);
    girls.add(anne);
    commitAndRotate();
    
    // acquire tables in different order than they were created (yes, we're mean)
    girls = snapshot.getTable("girls", Person.getTypeInfo());
    boys = snapshot.getTable("boys", Person.getTypeInfo());
    
    assertEquals("Boys", 1, boys.size());
    assertEquals("Girls", 1, girls.size());
    assertEquals("The boy", john, boys.first());
    assertEquals("The girl", anne, girls.first());
  }
  
  public void testRemoveRotate() throws Exception {
    try {
      table.add(john);
      table.add(anne);
      table.remove(john);
      commitAndRotate();
      assertEquals(1, table.size());
      assertEquals(anne, table.first());
    } catch (Exception e) {
      // Sometimes exceptions aren't displayed because of a subsequent exception in
      // tearDown(); so we display it here
      e.printStackTrace();
      throw e;
    }
  }
  
  public void testRemoveRotate2() throws Exception {
    table.add(john);
    table.add(anne);
    table.remove(anne);
    commitAndRotate();
    assertEquals(1, table.size());
    assertEquals(john, table.first());
  }
  
  public void testRotateRemove2() throws Exception {
    table.add(john);
    table.add(anne);
    commitAndRotate();
    table.remove(anne);
    assertEquals(1, table.size());
    assertEquals(john, table.first());
  }
  
  public void testAddAll() throws Exception {
    ArrayList<Person> v = new ArrayList<Person>();
    v.add(john);
    v.add(anne);
    table.addAll(v);
    assertEquals(2, table.size());
    assertEquals(john, table.get(0));
    assertEquals(anne, table.get(1));
  }
  
  public void testRetrieveSchema() throws Exception {
    rotate();
    TypeDef type = table.getSchema();
    assertEquals(new TypeDef("Person")
      .contentsAdd(new TypeComponent("string", "name"))
      .contentsAdd(new TypeComponent("long", "age")), type);
  }
  
  public void testSEEmptyTableConvertsToAnySchema() throws Exception {
    // do some operations on the table that end up with an empty table
    table.add(john);
    table.remove(john);
    snapshot.commit();
    
    // rotate store, load table with Song schema and add a song
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    Table<Song> table = snapshot.getTable("people", Song.getTypeInfo());
    table.add(constantSorrow);
    snapshot.commit();
    
    // assert song is still there
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("people", Song.getTypeInfo());
    assertEquals(constantSorrow, table.first());
  }
  
  public void testSECompletelyDifferentSchema() throws Exception {
    // add a person
    table.add(john);
    snapshot.commit();
    
    // rotate store, try to load table with Song schema
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    try {
      Table<Song> table = snapshot.getTable("people", Song.getTypeInfo());
      fail("No exception");
    } catch (SchemaChangeException e) {
      // ok
    }
  }
  
  public void testSEImpairedViewReadOnly() throws Exception {
    // add a person
    table.add(john);
    snapshot.commit();
    
    // rotate store, try to load table with subset schema
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    Table<JustName> table = snapshot.getTable("people", JustName.getTypeInfo());
    
    assertTableIsImmutable(table, ImmutableTableException.class, "age");
  }
  
  void assertTableIsImmutable(Table table, Class exceptionType, String messageSubstring) {
    for (int i = 0; i < 8; i++)
      try {
        switch (i) {
          case 0: table.add(null); break;
          case 1: table.removeAll((Filter) null); break;
          case 2: table.add(null); break;
          case 3: table.addAll(null); break;
          case 4: table.clear(); break;
          case 5: table.remove(null); break;
          case 6: table.remove(0); break;
          case 7: table.set(0, null); break;
        }
        fail("No exception");
      } catch (SmyleException e) {
        // exception is ok
        assertTrue("Wrong exception type: "+e, exceptionType.isInstance(e));
        assertTrue("'"+e.getMessage()+"' should contain '"+messageSubstring+"'",
          e.getMessage().indexOf(messageSubstring) >= 0);
      }
  }
  
  public void testImmutableSnapshot() throws Exception {
    Snapshot snapshot = store.snapshot();
    
    Table table = snapshot.getTable("people", Person.getTypeInfo());
    assertEquals(0, table.size());
    
    assertTableIsImmutable(table, ImmutableSnapshotException.class, "");
    
    // commit() and commitAndContinue() just does nothing on an immutable snapshot
    snapshot.commitAndContinue();
    snapshot.commit();
    
    /*for (int i = 0; i < 2; i++) try {
      switch (i) {
        case 0: snapshot.commit(); break;
        case 1: snapshot.commitAndContinue(); break;
      }
      fail("No exception");
    } catch (ImmutableSnapshotException e) {
      // ok
    }*/
  }
  
  public void testSEAddingFieldsOK() throws Exception {
    // add a person
    table.add(john);
    snapshot.commit();
    
    // rotate store, try to load table with extended schema
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    Table<ExtendedPerson> table = snapshot.getTable("people", ExtendedPerson.getTypeInfo());
    
    assertEquals(1, table.size());
    // male happens to be default gender (=0)
    assertEquals(new ExtendedPerson("John", 23, male), table.first());
    
    // add an (extended) row with non-default extended fields (gender in this case)
    ExtendedPerson lois = new ExtendedPerson("Lois", 27, female);
    table.add(lois);
    snapshot.commit();
    
    // rotate
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("people", ExtendedPerson.getTypeInfo());
    
    // assert john and lois are still here
    assertEquals(new ExtendedPerson("John", 23, male), table.first());
    assertEquals(lois, table.get(1));
  }
  
  public void testSEAddingManyFieldsOK() throws Exception {
    // add a person
    table.add(john);
    snapshot.commit();
    
    // rotate store, try to load table with extended schema
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    Table<VeryExtendedPerson> table = snapshot.getTable("people", VeryExtendedPerson.getTypeInfo());
    
    assertEquals(1, table.size());
    // male happens to be default gender (=0)
    assertEquals(new VeryExtendedPerson(null, "John", 23, false, male), table.first());
    
    // add an (extended) row with non-default extended fields (gender in this case)
    VeryExtendedPerson lois = new VeryExtendedPerson("Mrs.", "Lois", 27, true, female)
      .degreesAdd("Bachelor");
    table.add(lois);
    snapshot.commit();
    
    // rotate
    store.close();
    store = storeOpener.produce();
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("people", VeryExtendedPerson.getTypeInfo());
    
    // assert john and lois are still here
    assertEquals(new VeryExtendedPerson(null, "John", 23, false, male), table.first());
    assertEquals(lois, table.get(1));
  }
  
  public void testSEReorderedDataTypes() throws Exception {
    //store.setWriteLatency(0);

    DataTypes original = new DataTypes(true, 1, 2, "test", new JustName("john"))
      .longsAdd(3)
      .stringsAdd("test")
      .structsAdd(new JustName("anne"));
      
    ReorderedDataTypes reordered = new ReorderedDataTypes(new JustName("john"), "test", 2, 1, true)
      .longsAdd(3)
      .stringsAdd("test")
      .structsAdd(new JustName("anne"));
      
    // add a record
    {
      Table<DataTypes> table = snapshot.getTable("datatypes", DataTypes.getTypeInfo());
      table.add(original);
    }
      
    // commit, try to load table with modified schema
    commitAndGetNewSnapshot();
    
    {
      Table<ReorderedDataTypes> table = snapshot.getTable("datatypes", ReorderedDataTypes.getTypeInfo());
      assertEquals(reordered, table.first());
    
      // add the same row again
      table.add(reordered);
    }
    snapshot.commit();
    
    // commit, load table with original schema
    rotate();
    Table<DataTypes> table = snapshot.getTable("datatypes", DataTypes.getTypeInfo());
    
    // assert both rows are ok
    assertEquals(original, table.get(0));
    assertEquals(original, table.get(1));
  }
  
  public void testSENested() throws Exception {
    Nested original  = new Nested(new Person("John", 33), "what?");
    NestedX extended = new NestedX(new ExtendedPerson("John", 33, 0), "what?");
      
    // add a record
    {
      Table<Nested> table = snapshot.getTable("nested", Nested.getTypeInfo());
      table.add(original);
    }
      
    // commit, try to load table with modified schema
    commitAndGetNewSnapshot();
    
    {
      Table<NestedX> table = snapshot.getTable("nested", NestedX.getTypeInfo());
      assertEquals(extended, table.first());
    
      // add the same row again
      table.add(extended);
    }
    snapshot.commit();
    
    // commit, load table with original schema
    rotate();
    Table<Nested> table = snapshot.getTable("nested", Nested.getTypeInfo());
    
    // assert both rows are ok
    assertEquals(original, table.get(0));
    assertEquals(original, table.get(1));
  }
  
  public void testSERecursive() throws Exception {
    Recursive o1 = new Recursive(), o2 = new Recursive().recAdd(o1);
    RecursiveX n1 = new RecursiveX(), n2 = new RecursiveX().recAdd(n1);
    RecursiveX x1 = new RecursiveX().x("x1"), x2 = new RecursiveX().recAdd(x1).x("x2");
      
    // add a record
    {
      Table<Recursive> table = snapshot.getTable("recursive", Recursive.getTypeInfo());
      table.add(o2);
    }
      
    // commit, try to load table with modified schema
    commitAndGetNewSnapshot();
    
    {
      Table<RecursiveX> table = snapshot.getTable("recursive", RecursiveX.getTypeInfo());
      assertEquals(n2, table.first());
    
      table.add(x2);
    }
    snapshot.commit();
    
    // commit, load table with original schema
    rotate();
    Table<Recursive> table = snapshot.getTable("recursive", Recursive.getTypeInfo());
    
    // assert both rows are ok
    assertEquals(o2, table.get(0));
    assertEquals(o2, table.get(1));
  }
  
  public void testIterator() throws Exception {
    table.add(john);
    table.add(anne);
    Iterator<Person> i = table.iterator();
    TestUtil.checkIterator(i, new Person[] {john, anne});
  }
  
  public void testRemoveAllFilter() throws Exception {
    table.add(john);
    table.add(anne);
    table.removeAll(new Person_filter().nameEquals("Anne"));
    assertEquals(1, table.size());
    assertEquals(john, table.first());
  }
  
  public void testStoreDisabledAfterClose() throws Exception {
    store.close();
    try {
      for (int i = 0; i < 5; i++) try {
        switch (i) {
          // All the following operations must fail on a closed store
          
          case 0: store.close(); break;
          case 1: store.snapshot(); break;
          case 2: store.mutableSnapshot(); break;
          case 3: snapshot.getTable("people", Person.getTypeInfo()); break;
          case 4: snapshot.commit(); break;
          
          // Note: We don't check Table methods; they may fail or not
        }
        fail("No exception (i="+i+")");
      } catch (ClosedStoreException e) {
        // ok
      }
      
      // assert Snapshot.forget() is still allowed
      snapshot.forget();
    } finally {
      store = null;
    }
  }
  
  public void testSnapshotDisabledAfterForget() throws Exception {
    snapshot.forget();
    assertSnapshotDisabled();
  }
  
  public void testSnapshotDisabledAfterCommit() throws Exception {
    snapshot.commit();
    assertSnapshotDisabled();
  }
  
  void assertSnapshotDisabled() throws Exception {
    for (int i = 0; i < 3; i++) try {
      switch (i) {
        // All the following operations must fail on a disabled snapshot
        
        case 0: snapshot.commit(); break;
        case 1: snapshot.getTable("people", Person.getTypeInfo()); break;
        case 2: snapshot.commitAndContinue(); break;
        
        // Note: We don't check Table methods; they may fail or not
      }
      fail("No exception (i="+i+")");
    } catch (ClosedSnapshotException e) {
      // ok
    }
    
    snapshot.forget(); // should still be allowed
  }
  
  public void testCommitAndContinue() throws Exception {
    if (store.exclusiveWriteLocking()) return;
    
    Snapshot oldSnapshot = snapshot;
    Table<Person> oldTable = table;
    oldTable.add(john);
    oldSnapshot.commitAndContinue();
    
    // assert snapshot was committed
    getNewSnapshot();
    assertEquals(john, table.first());
    
    // assert we can continue working with the snapshot
    oldTable.add(anne);
    oldSnapshot.commit();
  }
  
  public void testGetTableTwice() throws Exception {
    Table<Person> table1 = snapshot.getTable("sometable", Person.getTypeInfo());
    Table<Person> table2 = snapshot.getTable("sometable", Person.getTypeInfo());
    assertTrue(table1 == table2);
  }
  
  public void testUniqueValues() throws Exception {
    table.add(john);
    table.add(anne);
    table.add(jane);
    IntVector ageList = table.getUniqueValues(Person.f_age);
    
    assertEquals(2, ageList.size());
    
    // order doesn't matter
    assertTrue(ageList.contains(21));
    assertTrue(ageList.contains(23));
  }
  
  public void testUniqueValueCountsInt() throws Exception {
    table.add(john);
    table.add(anne);
    table.add(jane);
    
    // without filter
    Map<Integer,Integer> map = table.getUniqueValueCounts(Person.f_age);
    assertEquals(2, map.size());
    assertEquals(new Integer(1), map.get(new Integer(21)));
    assertEquals(new Integer(2), map.get(new Integer(23)));
    
    // with filter
    map = table.getUniqueValueCounts(Person.f_age,
      new Person_filter().nameEquals("John"));
    assertEquals(1, map.size());
    assertEquals(new Integer(1), map.get(new Integer(23)));
  }
  
  public void testUniqueValueCounts() throws Exception {
    table.add(john);
    table.add(oldJohn);
    table.add(anne);
    
    Map<String,Integer> map = table.getUniqueValueCounts(Person.f_name);
    assertEquals(2, map.size());
    assertEquals(new Integer(2), map.get("John"));
    assertEquals(new Integer(1), map.get("Anne"));
  }
  
  public void testIndexOf() throws Exception {
    table.add(john);
    table.add(anne);
    assertEquals(0, table.indexOf(john));
    assertEquals(1, table.indexOf(anne));
    assertEquals(-1, table.indexOf(jane));
  }
  
  public void testSet() throws Exception {
    table.add(john);
    table.add(anne);
    table.set(0, anne);
    table.set(1, john);
    assertEquals(anne, table.get(0));
    assertEquals(john, table.get(1));
  }
  
  public void testGetOrCreate() throws Exception {
    Person_filter filter = new Person_filter().nameEquals("John");
    assertEquals(new Person().name("John"), table.getOrCreate(filter));
    table.add(john);
    table.add(anne);
    assertEquals(john, table.getOrCreate(filter));
  }
  
  public void testPut() throws Exception {
    Person_filter filter = new Person_filter().nameEquals("John");
    table.put(filter, john);
    assertEquals(1, table.size());
    assertEquals(john, table.get(0));
    
    Person oldJohn = new Person().name("John").age(49);
    table.put(filter, oldJohn);
    assertEquals(1, table.size());
    assertEquals(oldJohn, table.get(0));
  }
  
  public void testFailSafeIterators() throws Exception {
    // modifications that cause iterators to fail
    
    ArrayList<Person> v = new ArrayList<Person>();
    v.add(anne);
    
    for (int i = 0; i < 5; i++) {
      table.clear();
      table.add(john);
      Iterator<Person> it = table.iterator();
      switch (i) {
        case 0: table.add(anne); break;
        case 1: table.addAll(v); break;
        case 2: table.clear(); break;
        case 3: table.remove(john); break;
        case 4: table.remove(0); break;
      }
      assertIteratorFails(it);
    }
    
    // modifications that don't cause iterators to fail
    
    for (int i = 0; i < 2; i++) {
      table.clear();
      table.add(john);
      Iterator<Person> it = table.iterator();
      switch (i) {
        case 0: table.addAll(new ArrayList<Person>()); break;
        case 1: table.remove(anne); break;
      }
      TestUtil.checkIterator(it, new Person[] {john});
    }
  }
  
  public void testIteratorNoticesReplacedElements() throws Exception {
    table.add(john);
    Iterator<Person> it = table.iterator();
    assertTrue(it.hasNext());
    table.set(0, anne);
    assertEquals(anne, it.next());
  }
  
  void assertIteratorFails(Iterator<Person> it) {
    for (int i = 0; i < 2; i++) try {
      switch (i) {
        // All the following operations must fail
        
        case 0: it.hasNext(); break;
        case 1: it.next(); break;
        case 2: it.remove(); break;
      }
      fail("No exception (i="+i+")");
    } catch (ConcurrentModificationException e) {
      // ok
    }
  }
  
  public void testIteratorRemove() throws Exception {
    table.add(john);
    table.add(anne);
    Iterator<Person> it = table.iterator();
    assertEquals(john, it.next());
    it.remove();
    assertTrue(it.hasNext());
    assertEquals(anne, it.next());
    it.remove();
    assertTrue(!it.hasNext());
    assertEquals(0, table.size());
  }
  
  /** a thread that acquires a mutable snapshot and doesn't release it */
  class SnapshotKeeper extends Thread {
    Snapshot snapshot;
    Exception error;
    
    SnapshotKeeper() throws Exception {
      start();
    }

    void joinMe() throws Exception {
      join();
      if (error != null)
        throw error;
    }
    
    public void run() {
      try {
        snapshot = store.mutableSnapshot();
      } catch (Exception error) {
        this.error = error;
      }
    }
  }
    
  public void testWriteLockTimeout() throws Exception {
    snapshot.forget();
    
    SnapshotKeeper sk = new SnapshotKeeper();
    sk.joinMe();
    
    store.setTimeout(1); // timeout as quickly as possible

    try {
      store.logTo(null);
      Snapshot snapshot1 = store.mutableSnapshot();
      fail("No exception");
    } catch (TimeoutException e) {
      // ok
    }
  }
  
  public void testTwoThreadsWaitingForWriteLock() throws Exception {
    snapshot.forget();

    SnapshotKeeper sk1 = new SnapshotKeeper();
    sk1.joinMe();
    // sk1 now holds snapshot

    SnapshotKeeper sk2 = new SnapshotKeeper();
    SnapshotKeeper sk3 = new SnapshotKeeper();
    Thread.sleep(100); // should be enough for sk2 and sk3 to get into blocking mode

    sk1.snapshot.forget();

    // sk2 and sk3 should be able to acquire write lock now
    sk2.joinMe();
    Thread.sleep(100);
    sk2.snapshot.forget();
    sk3.joinMe();
  }

  public void testOnlyOneMutableSnapshotPerThread() throws Exception {
    if (!store.exclusiveWriteLocking()) return;
    
    try {
      store.logTo(null);
      store.mutableSnapshot();
      fail("No exception");
    } catch (MultipleMutableSnapshotsException e) {
      // ok
    }
  }
  
  public void testOrderByInt() throws Exception {
    table.add(oldJohn);
    table.add(john);
    table.add(anne);
    table.add(jane);
    // john and jane have same age, asserts that duplicates are allowed
    
    Iterator<Person> i = table.iterator(new Person_filter().orderByAge());
    assertTrue(i.hasNext());
    assertEquals(anne, i.next());
    assertTrue(i.hasNext());
    Person p = i.next();
    
    // order within same age is undefined
    if (p.equals(john))
      assertEquals(jane, i.next());
    else {
      assertEquals(jane, p);
      assertEquals(john, i.next());
    }
    
    TestUtil.checkIterator(i, new Person[] {oldJohn});
  }
  
  public void testOrderByString() throws Exception {
    table.add(john);
    table.add(jane);
    
    Iterator<Person> i = table.iterator(new Person_filter().orderByName());
    TestUtil.checkIterator(i, new Person[] {jane,john});
  }
  
  public void testOrderByTwoFields() throws Exception {
    table.add(john);
    table.add(anne);
    table.add(jane);
    
    Iterator<Person> i = table.iterator(new Person_filter().orderByAge().orderByName());
    TestUtil.checkIterator(i, new Person[] {anne,jane,john});
  }
  
  public void testOrderByLong() throws Exception {
    Table<DataTypes> table = snapshot.getTable("datatypes", DataTypes.getTypeInfo());
    DataTypes r1 = new DataTypes(), r2 = new DataTypes();
    r1.strct = new JustName();
    r2.strct = new JustName();
    r1.ll = 12345678901234L;
    r2.ll = -12345678901234L;
    table.add(r1);
    table.add(r2);
    
    Iterator<DataTypes> i = table.iterator(new DataTypes_filter().orderByLl());
    TestUtil.checkIterator(i, new DataTypes[] {r2,r1});
  }
  
  public void testOrderByReverse() throws Exception {
    table.add(john);
    table.add(jane);
    
    TestUtil.checkIterator(
      table.iterator(new Person_filter().orderByName().reverse()),
      new Person[] {john,jane});
    TestUtil.checkIterator(
      table.iterator(new Person_filter().reverse().orderByName()),
      new Person[] {john,jane});
    TestUtil.checkIterator(
      table.iterator(new Person_filter().reverse().orderByName().reverse()),
      new Person[] {jane,john});
  }
  
  public void testAutoIncrement() throws Exception {
    table.setAutoIncrementField(Person.f_age);
    table.add(new Person("one", 0));
    table.add(0, new Person("five", 5));
    table.add(1, new Person("three", 3));
    table.add(new Person("six", 0));
    TestUtil.checkIterator(table.iterator(), new Person[] {
      new Person("five", 5),
      new Person("three", 3),
      new Person("one", 1),
      new Person("six", 6)
    });
    
    commitAndRotate();
    table.add(new Person("seven", 0));
    TestUtil.checkIterator(table.iterator(), new Person[] {
      new Person("five", 5),
      new Person("three", 3),
      new Person("one", 1),
      new Person("six", 6),
      new Person("seven", 7)
    });
  }
  
  public void testAutoIncrementOnExistingTable() throws Exception {
    table.add(new Person("six", 0));
    table.add(new Person("five", 5));
    table.setAutoIncrementField(Person.f_age);
    table.add(new Person("seven", 0));
    TestUtil.checkIterator(table.iterator(), new Person[] {
      new Person("six", 6),
      new Person("five", 5),
      new Person("seven", 7)
    });
  }
  
  // tests every possible char from 0000 to FFFF excluding
  // D800-DBFF (that doesn't work in UTF-8 for some reason and
  // doesn't seem to contain any defined characters anyway)
  public void testFullUnicodeRange() throws Exception {
    StringBuffer buf = new StringBuffer(65536);
    for (int i = 0; i < 65536; i++)
      if (i < 0xD800 || i >= 0xDC00)
        buf.append((char) i);
    String s = buf.toString();
    /*System.out.println("Chars: "+s.length());
    System.out.println("Bytes: "+MCOP.stringToBytes(s).length);
    String s2 = MCOP.bytesToString(MCOP.stringToBytes(s));
    System.out.println("Chars: "+s2.length());*/
    table.add(new Person(s, 0));
    String s2 = table.first().name;
    assertEquals(s.length(), s2.length());
    for (int i = 0; i < s.length(); i++) {
      /*if ((int) s.charAt(i) != (int) s2.charAt(i))
        System.out.println("Char #"+i+": "+(int) s.charAt(i)+" vs "+(int) s2.charAt(i));*/
      assertEquals("Char #"+i, (int) s.charAt(i), (int) s2.charAt(i));
    }
  }

  DataTypes dt = new DataTypes(true, 1, 2, "test", new JustName("john"))
    .longsAdd(3)
    .stringsAdd("test")
    .structsAdd(new JustName("anne"));

  public void testDataTypesFilter() throws Exception {
    Table<DataTypes> table = snapshot.getTable("dt", DataTypes.getTypeInfo());
    table.add(dt);

    for (int i = 0; i < 6; i++) {
      DataTypes_filter filter = new DataTypes_filter()
        .bEquals    (i != 1 ? true : false)
        .lEquals    (i != 2 ? 1 : 2)
        .llEquals   (i != 3 ? 2 : 3)
        .sEquals    (i != 4 ? "test" : "x")
        .strctEquals(i != 5 ? new JustName("john") : new JustName("jane"));
      assertEquals(i == 0, table.contains(filter));
    }
  }
  
  public void testNullElement() throws Exception {
    for (int i = 0; i < 2; i++)
      try {
        if (i == 0)
          table.add(null);
        else {
          table.add(john);
          table.set(0, null);
        }
        fail("No exception");
      } catch (NullPointerException e) {
        // ok
      }
  }
  
  public void testWrongElementClass() throws Exception {
    JustName badElement = new JustName();
    
    for (int i = 0; i < 2; i++)
      try {
        if (i == 0)
          ((Table) table).add(badElement);
        else {
          table.add(john);
          ((Table) table).set(0, badElement);
        }
        fail("No exception");
      } catch (ClassCastException e) {
        // ok
      }
  }
  
  public void testOptimize() throws Exception {
    store.optimize();
    rotate();
  }
  
  public void testClearCaches() throws Exception {
    if (!canRotate) return;
    table.add(john);
    store.clearCaches();
    assertEquals(1, table.size());
    assertEquals(john, table.first());
    commitAndRotate();
    assertEquals(1, table.size());
    assertEquals(john, table.first());
  }
  
  public void testFreeImmutableSnapshot() throws Exception {
    Snapshot immutableSnapshot = store.snapshot();
    immutableSnapshot.forget();
    
    // do it again and check that it's usable
    immutableSnapshot = store.snapshot();
    immutableSnapshot.getTable("people", Person.getTypeInfo());
  }
}
