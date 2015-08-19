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
import drjava.smyle.meta.*;
import org.artsProject.mcop.core.*;
import org.artsProject.util.*;

/** tests garbage collection */
public class GCTest extends DiskStoreTestBase {
  public GCTest(String name) { super(name); }

  public void setUp() throws Exception {
    super.setUp();
    store.setWriteLatency(0);
  }
  
  void prepareGC() throws Exception {
    addPersons(table);
    // The next line is for growing the chunk table
    addMorePersons(table); removeMorePersons(table);
    snapshot.commit();
    gcCheckpointSmall();

    getNewSnapshot();
    addMorePersons(table);
    snapshot.commit();
    gcCheckpointBig();

    // remove the last 5 persons again
    getNewSnapshot();
    removeMorePersons(table);
    snapshot.commit();
  }
  
  /** complements prepareGC() */
  void assertGCWorked() throws Exception {
    // Note: By requiring an exact size match (rather than a <=) with the
    // store size before any GC, we implicitely assert
    // something else too: that a series of adds produces no garbage
    // (which it did in an early version).
    gcCheckpointSmallAgain(24*16, true); // allow some chunk table slack

    rotate();
    
    // assert table is still intact
    assertPersons(table);
  }

  void assertNoGC() throws Exception {
    gcCheckpointSmallAgain(4*16, false); // allow some chunk table slack
  }
  
  public void testGC() throws Exception {
    prepareGC();
    
    // We don't take a new snapshot here; so we automatically test
    // whether the current store state is respected by GC
    // (even if there's no snapshot of it)
    store.collectGarbage();
    assertGCWorked();
  }
  
  public void testGCRespectsSnapshots() throws Exception {
    addPersons(table);
    snapshot.commit();
    getImmutableSnapshot();
    Table oldTable = table;
    
    getNewSnapshot();
    table.clear();
    snapshot.commit();
    
    store.collectGarbage();
    assertPersons(oldTable);
  }
  
  public void testGCRespectsModifiedUncommittedSnapshots() throws Exception {
    if (store.exclusiveWriteLocking()) return;
    
    Table oldTable = table;
    addPersons(oldTable);
    
    getImmutableSnapshot();
    store.collectGarbage();
    assertPersons(oldTable);
  }
  
  void gcCheckpointSmall() {
    bytes1 = disk.totalBytes();
  }
  
  void gcCheckpointBig() {
    bytes2 = disk.totalBytes();
    assertTrue(bytes2+" > "+bytes1, bytes2 > bytes1);
  }
  
  void gcCheckpointSmallAgain(int increaseAllowed, boolean flag) {
    long bytes3 = disk.totalBytes();
    assertEquals(
      "Store should be at most "+bytes1+"+"+increaseAllowed+" bytes large, actual size: "+bytes3
      +" (files: "+disk.numberOfFiles()+"), max: "+bytes2, flag,
      bytes3 <= bytes1+increaseAllowed);
  }
  
  public void testGCAfterCrash() throws Exception {
    ((DefaultChunkManager) store.getChunkManager()).setNaturalFileSize(32);
    addPersons(table);
    snapshot.commit();
    gcCheckpointSmall();

    getNewSnapshot();
    addMorePersons(table);
    gcCheckpointBig();

    // simulate a crash by not committing and keeping the old store open
    createStore();
    gcCheckpointSmallAgain(16*16, true);

    // assert table is still intact and there was a GC
    assertPersons(table);
  }
  
  public void testGCIgnoresFreedSnapshots() throws Exception {
    testMemoryGC(true);
  }
  
  public void testGCPerformsMemoryGC() throws Exception {
    testMemoryGC(false);
  }
  
  void testMemoryGC(boolean callMemoryGC) throws Exception {
    //if (store.exclusiveWriteLocking()) return;
    
    ((DefaultChunkManager) store.getChunkManager()).setNaturalFileSize(32);
    commitAndGetNewSnapshot();
    gcCheckpointSmall();
    
    addPersons(table);
    gcCheckpointBig();
    
    // acquire immutable snapshot
    Snapshot oldSnapshot = store.snapshot();
    
    removePersons(table);
    snapshot.commit();
    snapshot = null;
    // table is now small again; but old snapshot still references
    // obsolete data
    
    oldSnapshot = null; // drop reference to old snapshot
    if (callMemoryGC)
      System.gc();
    store.collectGarbage();
    gcCheckpointSmallAgain(8*16, true);
  }
  
  public void testGCIgnoresForgottenSnapshots() throws Exception {
    ((DefaultChunkManager) store.getChunkManager()).setNaturalFileSize(32);
    commitAndGetNewSnapshot();
    gcCheckpointSmall();
    
    addPersons(table);
    gcCheckpointBig();
    
    snapshot.forget();
    getImmutableSnapshot();
    
    store.collectGarbage();
    gcCheckpointSmallAgain(7*16, true);
  }
  
  public void testGCOnClose() throws Exception {
    ((DefaultChunkManager) store.getChunkManager()).setNaturalFileSize(32);
    // The next line is for growing the chunk table
    addMorePersons(table); removeMorePersons(table);
    commitAndGetNewSnapshot();
    gcCheckpointSmall();
    
    addPersons(table);
    commitAndGetNewSnapshot();
    Snapshot bigSnapshot = store.snapshot(); // retain reference, don't forget()
    gcCheckpointBig();
    
    table.clear();
    commitAndGetNewSnapshot();
    
    store.close();
    gcCheckpointSmallAgain(4*16, true); // allow some chunk table slack
    
    store = null;
  }
  
  public void testGCReusedChunks() throws Exception {
    table.add(john);
    snapshot.commit();
    getImmutableSnapshot();
    Table oldTable = table;
    
    getNewSnapshot();
    table.clear();
    table.add(anne);
    snapshot.commit();
    
    store.collectGarbage();
    assertEquals(john, oldTable.first());
    assertEquals(anne, table.first());
  }
  
  public void testGCAcquiresWriteLock() throws Exception {
    if (!store.exclusiveWriteLocking()) return;
    
    try {
      store.logTo(null);
      store.collectGarbage();
      fail("No exception");
    } catch (MultipleMutableSnapshotsException e) {
      // ok
    }
  }
  
  public void testAutoGC() throws Exception {
    store.setWriteLatency(0);
    store.setGCFrequency(1001);
    store.setClusterSize(1000);
    prepareGC();
    //assertTrue("Total bytes written: "+disk.totalBytesWritten(), disk.totalBytesWritten() >= 1000);
    assertGCWorked();
    
    // assert it works twice too
    
    // set parameters again because prepareGC rotated store
    store.setWriteLatency(0);
    store.setGCFrequency(1001);
    store.setClusterSize(1000);
    prepareGC();
    //assertTrue("Total bytes written: "+disk.totalBytesWritten(), disk.totalBytesWritten() >= 1000*2);
    
    // assertGCWorked won't do here because tables has 10 elements, not 5
    gcCheckpointSmallAgain(4*16, true);
  }
  
  public void testNoAutoGC() throws Exception {
    store.setGCFrequency(10000);
    store.setClusterSize(1000);
    prepareGC();
    assertTrue("Total bytes written: "+disk.totalBytesWritten(), disk.totalBytesWritten() < 10000);
    assertNoGC();
  }
}
