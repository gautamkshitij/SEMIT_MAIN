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
import org.artsProject.mcop.core.*;
import drjava.gjutil.Factory;
import org.artsProject.util.*;

class StoreTestBase extends TestCase {
  Factory<Store> storeCreator, storeOpener;
  boolean canRotate;
  Store store;
  Snapshot snapshot;
  Table<Person> table;
  
  Song
    constantSorrow = new Song("The Soggy Bottom Boys", "I Am A Man Of Constant Sorrow");
  
  static final boolean debug = false;
  
  Person
    john = new Person("John", 23),
    anne = new Person("Anne", 21),
    jane = new Person("Jane", 23),
    oldJohn = new Person("John", 52);
    
  static final int male = 0, female = 1;
    
  public StoreTestBase(String name) {
    super(name);
  }

  public void init(Factory<Store> storeCreator, Factory<Store> storeOpener,
    boolean canRotate) {
    this.storeCreator = storeCreator;
    this.storeOpener = storeOpener;
    this.canRotate = canRotate;
  }
  
  public void setUp() throws Exception {
    createStore(true);
  }
  
  void createStore(boolean wipe) throws Exception {
    store = (wipe ? storeCreator : storeOpener).produce();
    getNewSnapshot();
  }
  
  void rotate() throws Exception {
    if (store != null) store.close();
    createStore(false);
  }
  
  public void tearDown() throws Exception {
    if (store != null) store.close();
  }
  
  void getNewSnapshot() throws Exception {
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("people", Person.getTypeInfo());
  }
  
  void commitAndGetNewSnapshot() throws Exception {
    snapshot.commit();
    getNewSnapshot();
  }
  
  void commitAndRotate() throws Exception {
    snapshot.commit();
    rotate();
  }
}
