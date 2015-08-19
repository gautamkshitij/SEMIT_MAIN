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

public class ScalablePoolTest extends TestCase {
  public ScalablePoolTest(String name) { super(name); }
  
  ChunkManager cm;
  ScalablePool<String> pool;
  int id1, id2;
  
  public void setUp() throws Exception {
    cm = new DefaultChunkManager(new MemoryDisk(), DiskStore.VERSION);
    pool = new ScalablePool<String>(new StringMarDemar(), cm, null);
  }
  
  public void testAddGetReplace() throws Exception {
    for (int i = 0; i < 128; i++)
      assertEquals(i, pool.insert(String.valueOf(i)));
    for (int i = 0; i < 128; i++)
      assertEquals(String.valueOf(i), pool.get(i));
    for (int i = 0; i < 128; i++)
      if ((i & 1) == 0)
        pool.replace(i, String.valueOf(i*2));
      else
        pool.release(i);
      
    checkAfterAddGetReplace();
  }
  
  void checkAfterAddGetReplace() throws Exception {
    for (int i = 0; i < 128; i++)
      assertEquals((i & 1) == 0 ? String.valueOf(i*2) : null, pool.get(i));
    assertEquals(128, pool.size());
    // test get with a very high, not-existing index
    assertEquals(null, pool.get(123456));
    pool.checkConsistency();
  }
  
  public void testReleaseWholeBlock() throws Exception {
    for (int i = 0; i < 128; i++)
      assertEquals(i, pool.insert(String.valueOf(i)));
    for (int i = 31; i >= 0; i--)
      pool.release(i);
    assertEquals(null, pool.get(0));
    for (int i = 0; i < 32; i++)
      assertEquals(i, pool.insert(String.valueOf(i)));
  }
  
  public void testNonExtantIds() {
    for (int i = -10; i < 10; i++)
      assertNull(pool.get(i));
  }

  public void testInsert() {
    int id = pool.insert("apple");
    assertEquals("apple", pool.get(id));
  }
  
  public void testInsertTwoObjects() {
    id1 = pool.insert("apple");
    id2 = pool.insert("pear");
    assertEquals("apple", pool.get(id1));
    assertEquals("pear", pool.get(id2));
  }
  
  public void testGetIsRepeatable() {
    testInsertTwoObjects();
    assertEquals("apple", pool.get(id1));
    assertEquals("pear", pool.get(id2));
  }
  
  public void testReleasedSlotsContainNull() {
    int id = pool.insert("banana");
    pool.release(id);
    assertNull(pool.get(id));
  }
  
  public void testIdRecycling() {
    int id = pool.insert("banana");
    pool.release(id);
    int id2 = pool.insert("pineapple");
    assertEquals("ID", id, id2);
  }
  
  /** more complicated recycling test
      (fails if ids can only be recycled from the end) */
  public void testIdRecycling2() {
    testInsertTwoObjects();
    pool.release(id1);
    pool.release(id2);
    int id3 = pool.insert("orange");
    int id4 = pool.insert("grapefruit");
    assertEquals("ID 1", id1, id3);
    assertEquals("ID 2", id2, id4);
  }
  
  /** this is needed for DispatcherTest */
  public void testFirstIdIsZero() {
    assertEquals(0, pool.insert("something"));
  }
  
  public void testReplace() {
    testInsertTwoObjects();
    pool.replace(id1, "papaya");
    assertEquals("papaya", pool.get(id1));
    assertEquals("pear", pool.get(id2));
  }
  
  public void testSaveAndLoad() throws Exception {
    testAddGetReplace();
    rotate();
    checkAfterAddGetReplace();
  }
  
  void rotate() throws Exception {
    ChunkRef master = pool.flush();
    pool = new ScalablePool<String>(new StringMarDemar(), cm, master);
  }
}
