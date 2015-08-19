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
import java.lang.reflect.*;
import junit.framework.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;
import drjava.smyle.core.*;
import org.artsProject.mcop.*;
import drjava.util.*;

public class DiskStoreTestBase extends XTestCase {
  public DiskStoreTestBase(String name) { super(name); }
  
  Person
    john  = new Person("John",  23),
    anne  = new Person("Anne",  21),
    jane  = new Person("Jane",  23),
    maria = new Person("Maria", 45);
    
  MemoryDisk disk;
  DiskStore store;
  Snapshot snapshot;
  Table<Person> table;
  long bytes1, bytes2;
  
  public void setUp() throws Exception {
    disk = new MemoryDisk();
    createStore();
  }

  public void tearDown() throws Exception {
    if (store != null) store.kill();
  }
  
  void getNewSnapshot() throws Exception {
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("person", Person.getTypeInfo());
  }
  
  void getImmutableSnapshot() throws Exception {
    snapshot = store.snapshot();
    table = snapshot.getTable("person", Person.getTypeInfo());
  }
  
  void commitAndGetNewSnapshot() throws Exception {
    snapshot.commit();
    getNewSnapshot();
  }
  
  void createStore() throws Exception {
    if (store != null) store.kill();
    store = new DiskStore(disk);
    store.setTimeout(5000);
    getNewSnapshot();
  }
  
  void rotate() throws Exception {
    if (store != null) store.close();
    createStore();
  }
  
  void addPersons(Table table) throws Exception {
    for (int i = 1; i <= 5; i++) table.add(new Person("someone", i));
  }
  
  void addMorePersons(Table table) throws Exception {
    for (int i = 6; i <= 10; i++) table.add(new Person("someone", i));
  }
  
  void removePersons(Table table) throws Exception {
    for (int i = 1; i <= 5; i++) table.remove(new Person("someone", i));
  }
  
  void removeMorePersons(Table table) throws Exception {
    for (int i = 6; i <= 10; i++) table.remove(new Person("someone", i));
  }
  
  void assertPersons(Table table) throws Exception {
    assertEquals(5, table.size());
    for (int i = 1; i <= 5; i++)
      assertEquals(table.get(i-1), new Person("someone", i));
  }
}
