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
import drjava.smyle.*;
import drjava.smyle.core.StoreRegistry;
import drjava.smyle.core.DiskStore;
import drjava.util.*;

public class SmyleTest extends TestCase {
  public SmyleTest(String name) { super(name); }
  
  public void testTwoStoresOnSameDir() throws Exception {
    // use two different names for the same dir
    Store store1 = Smyle.createEmptyStore("temp/db");
    Store store2 = Smyle.openStore("temp/../temp/db");
    
    try {
      assert("Stores should be identical", store1 == store2);
    } finally {
      store1.close();
      store2.close();
    }
  }
  
  public void testTwoStoresOnDifferentDirs() throws Exception {
    // use two different dirs
    Store store2 = Smyle.createEmptyStore("temp/db2");
    store2.close();
    Store store1 = Smyle.createEmptyStore("temp/db");
    store2 = Smyle.openStore("temp/db2");
    
    try {
      assert("Stores should not be identical", store1 != store2);
    } finally {
      store1.close();
      store2.close();
    }
  }
  
  public void testStoreForgottenOnClose() throws Exception {
    Store store1 = Smyle.createEmptyStore("temp/db");
    store1.close();
    Store store2 = Smyle.openStore("temp/db");
    
    try {
      assert("Stores should not be identical", store1 != store2);
    } finally {
      store2.close();
    }
  }
  
  public void testStoreRegistryIsSynchronized() throws Exception {
    SynchronisationTester.assertPublicMethodsAreSynchronized(StoreRegistry.class);
  }

  public void testReadOnlyStore() throws Exception {
    // create and open store, bypassing registry
    Store store = new DiskStore(new File("temp/db"));

    // open read-only store while normal store is already open
    Store store2 = Smyle.openStoreReadOnly("temp/db");
 
    try {
      store2.mutableSnapshot();
      fail("No exception");
    } catch (ReadOnlyException e) {
      // ok
    }
 
    store2.close();
    store.close();
  }

  public void testStoreExists() throws Exception {
    assertEquals(false, Smyle.storeExists("temp/nostorehere"));

    Smyle.createEmptyStore("temp/db").close();
    Store store = Smyle.openStore("temp/db");
    assertEquals(true, Smyle.storeExists("temp/db"));
    assertEquals(true, Smyle.storeInUse("temp/db"));
    store.close();
    assertEquals(true, Smyle.storeExists("temp/db"));
    assertEquals(false, Smyle.storeInUse("temp/db"));
  }
}
