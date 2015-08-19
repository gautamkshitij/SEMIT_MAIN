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
import drjava.smyle.*;
import drjava.smyle.core.*;
import org.artsProject.mcop.*;
import junit.framework.*;
import drjava.util.*;

public class ChunkManagerTest extends TestCase {
  public ChunkManagerTest(String name) { super(name); }
  
  MemoryDisk disk;
  DefaultChunkManager cm;
  ChunkRef chunk1, chunk2;
  
  public void setUp() throws Exception {
    disk = new MemoryDisk();
    rotate();
  }
  
  void rotate() {
    cm = new DefaultChunkManager(disk, DiskStore.VERSION);
  }
  
  Buffer data1() {
    Buffer b = new Buffer();
    for (int i = 0; i < 64; i++)
      b.writeLong(123);
    return b;
  }
  
  Buffer data2() {
    Buffer b = new Buffer();
    for (int i = 0; i < 64; i++)
      b.writeLong(432);
    return b;
  }
  
  public void testWriteReadMasterChunk() throws Exception {
    ChunkRef chunk = cm.createMasterChunk(data1());
    
    assertNotNull(chunk);
    assertEquals(chunk, cm.getMasterChunk());
    assertEquals(data1().toString(), cm.readChunk(chunk).toString());
    assertEquals(3, disk.numberOfFiles());
  }
  
  public void testWriteReadMasterChunkTwice() throws Exception {
    chunk1 = cm.createMasterChunk(data1());
    chunk2 = cm.createMasterChunk(data2());
    
    assertEquals(chunk2, cm.getMasterChunk());
    assertEquals(data1().toString(), cm.readChunk(chunk1).toString());
    assertEquals(data2().toString(), cm.readChunk(chunk2).toString());
    
    assertEquals(6, disk.numberOfFiles());
  }
  
  public void testWriteReadMasterChunkRotate() throws Exception {
    ChunkRef chunk = cm.createMasterChunk(data1());
    rotate();
    assertEquals(data1().toString(), cm.readChunk(cm.getMasterChunk()).toString());
  }
  
  void testNormalAndMasterChunk(boolean rotate) throws Exception {
    chunk1 = cm.createChunk(data1());
    chunk2 = cm.createMasterChunk(data2());
    
    if (rotate) rotate();
    assertEquals(chunk2, cm.getMasterChunk());
    assertEquals(data1().toString(), cm.readChunk(chunk1).toString());
    assertEquals(data2().toString(), cm.readChunk(chunk2).toString());
    
    // asserts that both chunks were combined in one file
    assertEquals(3, disk.numberOfFiles());
  }
  
  public void testNormalAndMasterChunk() throws Exception {
    testNormalAndMasterChunk(false);
  }
  
  public void testNormalAndMasterChunkRotate() throws Exception {
    testNormalAndMasterChunk(true);
  }
  
  public void testExactNaturalFileSize() throws Exception {
    cm.setNaturalFileSize(4*64*2);
    chunk1 = cm.createChunk(data1());
    chunk2 = cm.createMasterChunk(data2());
    assertEquals(data1().toString(), cm.readChunk(chunk1).toString());
    assertEquals(data2().toString(), cm.readChunk(chunk2).toString());
    assertEquals(3, disk.numberOfFiles());
  }
  
  public void testNaturalFileSize() throws Exception {
    cm.setNaturalFileSize(4*64+32);
    chunk1 = cm.createChunk(data1());
    chunk2 = cm.createMasterChunk(data2());
    assertEquals(data1().toString(), cm.readChunk(chunk1).toString());
    assertEquals(data2().toString(), cm.readChunk(chunk2).toString());
    assertEquals(4, disk.numberOfFiles());
  }
  
  public void testNoMasterChunk() throws Exception {
    ChunkRef chunk = cm.getMasterChunk();
    assertNotNull(chunk);
    assertEquals(0, chunk.index);
  }
  
  public void testChunkManagerIsSynchronized() throws Exception {
    SynchronisationTester.assertPublicMethodsAreSynchronized(cm.getClass());
  }
}
