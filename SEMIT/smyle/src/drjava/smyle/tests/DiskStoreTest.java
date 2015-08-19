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

/** tests how DiskStore interacts with the Disk */

public class DiskStoreTest extends DiskStoreTestBase {
  public DiskStoreTest(String name) { super(name); }
  
  public void testFailureWhileWritingMaster() throws Exception {
    store.setWriteLatency(0);

    // add John
    table.add(john);
    snapshot.commitAndContinue();
    
    // try to remove John, but don't allow to write master
    disk.sabotageMasterWrite = true;
    
    try {
      table.clear();
      snapshot.commit();
      fail("Where's the exception?");
    } catch (SmyleIOException e) {
      // We simulate an application that catches the exception, displays an
      // error or whatever and goes on
    }
    snapshot.forget();
    
    // assert there was a proper rollback
    getImmutableSnapshot();
    assertEquals(john, table.first());
  }
  
  /** not a real test, but a utility method */
  /*public void test_printMarshalledDisk() throws Exception {
    table.add(john);
    table.add(anne);
    snapshot.commit();
    Buffer buffer = new Buffer();
    disk.marshal(buffer);
    System.out.println("Disk: "+buffer);
  }*/
  
  void testSerializedStore(String hex) throws Exception {
    disk = new MemoryDisk(new Buffer(hex));
    
    int oldGCFreq = DiskStore.DEFAULT_GC_FREQUENCY;
    DiskStore.DEFAULT_GC_FREQUENCY = 1;
      // make sure GC occurs during conversion (exposes a certain bug)
      
    try {
      createStore();
    } finally {
      DiskStore.DEFAULT_GC_FREQUENCY = oldGCFreq;
    }
    
    table.add(maria);
    snapshot.commit();
    rotate();
    assertEquals(table.size(), 3);
    assertEquals(john, table.get(0));
    assertEquals(anne, table.get(1));
    assertEquals(maria, table.get(2));
  }
  
  /** load a Smyle 0.1.0 store */
  public void test010Store() throws Exception {
    testSerializedStore(
      "00000004000000040000001B00000001000000010000000300000001000000"+
      "07706572736F6E00000000030000000C000000020000000100000002000000020000000D00000005"+
      "416E6E650000000015000000010000000D000000054A6F686E00000000170000000400000004");
  }
      
  /** load a Smyle 0.2 store */
  public void test02Store() throws Exception {
    testSerializedStore(
      "00000005000000050000001B00000002000000010000000400000001000000"+
      "07706572736F6E00000000040000001000000001000000020000000200000003000000030000000D"+
      "00000005416E6E650000000015000000020000000D000000054A6F686E0000000017000000010000"+
      "004B0000001253746F7265546573743A3A506572736F6E000000000200000007737472696E670000"+
      "0000056E616D650000000000000000056C6F6E670000000004616765000000000000000000000000"+
      "0500000005");
  }

  /** load a Smyle 0.2.5 store */
  public void test025Store() throws Exception {
    testSerializedStore(
      "00000003000000030000005C00000019000000050000000000000002000000"+
      "000000004B00000000000000020000004B0000000D0000000000000002000000580000000D000000"+
      "000000000200000065000000100000000000000002000000750000001B0000000500000002000000"+
      "900000001253746F7265546573743A3A506572736F6E000000000200000007737472696E67000000"+
      "00056E616D650000000000000000056C6F6E67000000000461676500000000000000000000000005"+
      "4A6F686E000000001700000005416E6E650000000015000000010000000200000002000000030000"+
      "001900000001000000040000000100000007706572736F6E00000000010000000C00000019000000"+
      "00000000000000000300000003");
  }

  /** load a Smyle 0.8 store */
  public void test08Store() throws Exception {
    testSerializedStore(
      "00000003000000030000003400000123000000020000000300000000000000020000000000"+
      "0000540000000000000002000000540000000C000000020000000500000002000000600000001F00"+
      "0000000000000100000000000000400000000000000001000000400000000D000000000000000100"+
      "00004D0000000D00000000000000010000005A000000180000000000000001000000720000001B00"+
      "0000010000000100000001000000010000008D00000007506572736F6E0000000002000000077374"+
      "72696E6700000000056E616D650000000000000000056C6F6E670000000004616765000000000000"+
      "000000000000054A6F686E000000001700000005416E6E6500000000150000000100000002000000"+
      "0200000003FFFFFFFF000000000000012300000001000000040000000100000007706572736F6E00"+
      "0000000300000003");
  }
  
  public void testDiskStoreIsSynchronized() throws Exception {
    SynchronisationTester.assertPublicMethodsAreSynchronized(store.getClass());
  }
  
  public void testSnapshotIsSynchronized() throws Exception {
    // Assert that SnapshotImpl methods called by TableImpl are synchronized too
    SynchronisationTester.assertPackageAndPublicMethodsAreSynchronized(snapshot.getClass());
  }
  
  // This doesn't work anymore because table methods synchronize on
  // the snapshot now
  /*public void testTableIsSynchronized() throws Exception {
    SynchronisationTester.assertPackageAndPublicMethodsAreSynchronized(table.getClass(),
      new SynchronisationTester.MethodPredicate() {
        // exclude gj bridge methods
        public boolean evaluate(Method m) {
          if (m.getReturnType() == Object.class)
            return false;
          Class[] params = m.getParameterTypes();
          for (int i = 0; i < params.length; i++)
            if (params[i] == Object.class || params[i] == new Object[0].getClass())
              return false;
          return true;
        }
      });
  }*/
  
  /*public void testMultipleProcessAccessDetected() throws Exception {
  }*/

  public void testDeferredWrites() throws Exception {
    store.setWriteLatency(50);
    table.add(maria);
    snapshot.commit();
    Thread.sleep(200);
    createStore();
    assertEquals(1, table.size());
  }

  public void testReadOnlyDisk() throws Exception {
    tearDown();
    disk = new MemoryDisk();

    // Trying to open a non-existing store in read-only mode should fail
    try {
      new DiskStore(disk, true);
      fail("No exception");
    } catch (StoreNotFoundException e) {
      // ok
    }

    // create a store on disk
    new DiskStore(disk, false).close();

    disk.readOnly = true;
    store = new DiskStore(disk, true);

    store.snapshot(); // immutable snapshots are ok

    try {
      store.mutableSnapshot();
      fail("No exception");
    } catch (ReadOnlyException e) {
      // ok
    }

    store.close();
  }
}
