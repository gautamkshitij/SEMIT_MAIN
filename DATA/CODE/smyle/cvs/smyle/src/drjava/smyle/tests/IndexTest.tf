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

// IndexTest.tf
// Smyle test script for index handling
// This is a Beanshell script that uses the drjava.tf framework

import drjava.smyle.core.*;
import drjava.smyle.testtypes.*;

// test records
john  = new Person("John",  23);
anne  = new Person("Anne",  21);
jane  = new Person("Jane",  23);
maria = new Person("Maria", 45);

void setUp() {
  // create store, snapshot, table
  super.store = new DiskStore(new MemoryDisk());
  super.snapshot = store.mutableSnapshot();
  super.table = snapshot.getTable("person", Person.getTypeInfo());
}

void addRecords() {  
  table.add(john);
  table.add(anne);
  table.add(jane);
}

void indexAge() {
  fields = new ArrayList();
  fields.add(Person.f_age);
  table.addIndex(fields);
  super.idx = table.getIndex(0);
}

void indexName() {
  fields = new ArrayList();
  fields.add(Person.f_name);
  table.addIndex(fields);
  super.idx = table.getIndex(0);
}

void testOrderByOverIndex() {
  addRecords();
  indexAge();
  checkIterator(
    table.iterator(new Person_filter().orderByAge()),
    new Person[] {anne, john, jane}); // order of john/jane is undefined
  checkIterator(
    table.iterator(new Person_filter().orderByAge().reverse()), 
    new Person[] {jane, john, anne}); // order of john/jane is undefined
}

void testIndexConstruction() {
  table.add(john);
  indexAge();
  checkIndex(new UniversalKey(john.age));
}

void testAddField() {
  table.add(john);
  indexAge();
  idx.addField(Person.f_name);
  checkIndex(UniversalKey.concatDimensions(new Object[] { john.age, "John" }));
}

void checkIndex(UniversalKey expectedKey) {
  // check tree contents
  MapIterator i = idx.getTree().iterate();
  assertTrue(i.hasNext());
  i.next(); // don't check element...
  assertEquals(expectedKey, i.getKey());
  assertFalse(i.hasNext());
}

void prepareAutoIndexing() {
  table.getIndexAdvisor().setInitialHurdle(1);
  table.add(john);
  table.add(anne);
}

void testIntAutoIndexing() {
  prepareAutoIndexing();

  // iterate to trigger index creation
  checkIterator(table.iterator(new Person_filter().orderByAge()), new Person[] {anne, john});
  assertEquals(1, table.numIndexes());
}

void testRemoveAllOverCutIndex() {
  table.setMaxKeyLength(4);
  table.add(john);
  indexName();
  table.add(new Person("Horst", 45));
  table.removeAll(new Person_filter().nameEquals("Horst"));
  checkIterator(table.iterator(), new Person[] {john});
}

void testIndexOfOverIndex() {
  addRecords();
  indexAge();
  
  assertEquals(0, table.indexOf(new Person_filter().ageEquals(23)));
  assertEquals(1, table.indexOf(new Person_filter().ageEquals(21)));
  assertEquals(-1, table.indexOf(new Person_filter().ageEquals(99)));
}

void testLongStringIndex() {
  indexName();
  table.add(new Person("Hadji Ben Alef Omar, Prinz von Persien", 30));
  //checkIterator(table.iterator(), new Person[] {john});
}

