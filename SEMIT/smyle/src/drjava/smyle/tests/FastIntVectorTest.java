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

import java.util.*;
import junit.framework.*;
import org.artsProject.mcop.*;
import drjava.smyle.core.*;

public class FastIntVectorTest extends TestCase {
  public FastIntVectorTest(String name) { super(name); }
  
  final int blockSize = 4;
  
  FastIntVector v;
  ArrayList<Integer> vRef;
  //ChunkManager cm;
  BitSet whiteList;
  Random random;
  
  public void setUp() throws Exception {
    //cm = new DefaultChunkManager(new MemoryDisk(), DiskStore.VERSION);
    v = new FastIntVector();
    v.setBlockSize(blockSize);
    vRef = new ArrayList<Integer>();
    whiteList = new BitSet();
    random = new Random(0);
  }
  
  public void testAddAndGet() throws Exception {
    for (int i = 0; i < 20; i++)
      add(i*2);
    for (int i = 0; i < 20; i++)
      assertEquals(i*2, v.get(i));
    assertContentsAndStructure();
  }

  public void testRandomAdd() throws Exception {
    for (int i = 0; i < 16; i++) {
      add((random.nextInt() & 0x7fffffff) % (vRef.size()+1), i*1000);
      assertContentsAndStructure();
    }
  }

  public void testRemoveElement() throws Exception {  
    for (int i = 0; i < 32; i++)
      add(i*2);
    assertTrue(!removeElement(-123));
    for (int i = 0; i < 32; i++) {
      assertTrue(removeElement((i^5)*2));
      assertContentsAndStructure();
    }
  }
  
  public void testRemoveZeroBug() throws Exception {
    add(1);
    assertTrue(!v.removeElement(0));
  }
  
  public void testRemove() throws Exception {  
    for (int i = 0; i < 32; i++)
      add(i*2);
    for (int i = 0; i < 32; i++) {
      remove((random.nextInt() & 0x7fffffff) % vRef.size());
      assertContentsAndStructure();
    }
  }
  
  public void testSet() throws Exception {  
    for (int i = 0; i < 32; i++)
      add(i*2);
    for (int i = 31; i >= 0; i--) {
      set(i, i*3);
      assertContentsAndStructure();
    }
  }
  
  public void testIterator() throws Exception {  
    assertTrue(!v.iterator().hasNext());
    
    for (int i = 0; i < 32; i++)
      add(i*2);
      
    Iterator<Integer> iRef = vRef.iterator();
    for (IntIterator i = v.iterator(); i.hasNext(); )
      assertEquals(iRef.next().intValue(), i.next());
    assertTrue(!iRef.hasNext());
  }
  
  void add(int i) {
    v.add(i);
    vRef.add(new Integer(i));
  }
  
  void add(int index, int value) {
    v.add(index, value);
    vRef.add(index, new Integer(value));
  }
  
  void set(int index, int value) {
    v.set(index, value);
    vRef.set(index, new Integer(value));
  }
  
  boolean removeElement(int i) {
    vRef.remove(new Integer(i));
    return v.removeElement(i);
  }
  
  void remove(int index) {
    v.remove(index);
    vRef.remove(index);
  }
  
  void rotate() throws Exception {
    /*ChunkRef master = vector.flush();
    BitSet oldWhiteList = (BitSet) whiteList.clone();
    vector.collectChunks(whiteList);
    cm.deleteEverythingBut(whiteList);
    whiteList = oldWhiteList;
    vector = new FastIntVector<HString>(cm, new HStringMarDemar(), master);*/
  }
  
  void assertContentsAndStructure() {
    assertContentsAndStructure(blockSize/2);
  }
  
  void assertContentsAndStructure(int minBlockSize) {
    assertEquals(vRef.size(), v.size());
    assertEquals(vRef.isEmpty(), v.isEmpty());
    int n = 0;
    for (int b = 0; b < v.numBlocks(); b++) {
      int[] data = v.getBlockData(b);
      if (!(vRef.size() <= blockSize && data.length == vRef.size()))
        assert("Data length doesn't fit block size "+blockSize+": "+data.length,
          data.length >= minBlockSize && data.length <= blockSize);
      for (int i = 0; i < data.length; i++)
        assertEquals(vRef.get(n++).intValue(), data[i]);;
    }
    assertEquals("Total entries", vRef.size(), n);
  }
  
  public void testChunkRefSeqCompatibility() {
    // load from buffer
    Buffer b = new Buffer();
    ArrayList<ChunkRef> vCR = new ArrayList<ChunkRef>();
    for (int i = 0; i < 16; i++) {
      vCR.add(new ChunkRef(i+9));
      vRef.add(new Integer(i+9));
    }
    MCOP.writeSeq(b, vCR);
    String data1 = b.toString();
    v.read(b);
    assertContentsAndStructure(blockSize);
    
    // save to buffer
    b = new Buffer();
    v.marshal(b);
    assertEquals(data1, b.toString());
  }
  
  public void testClear() throws Exception {
    v.add(55);
    v.clear();
    assertContentsAndStructure();
  }
  
  public void testIndexOf() throws Exception {
    for (int i = 0; i < 32; i++) v.add(i*3);
    assertEquals(0, v.indexOf(0));
    assertEquals(16, v.indexOf(16*3));
    assertEquals(31, v.indexOf(31*3));
    assertEquals(-1, v.indexOf(32*3));
  }
}
