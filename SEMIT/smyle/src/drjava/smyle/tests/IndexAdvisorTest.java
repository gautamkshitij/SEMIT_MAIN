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
import drjava.smyle.core.*;

public class IndexAdvisorTest extends TestCase {
  public IndexAdvisorTest(String name) { super(name); }
  
  IndexAdvisor<String> advisor;
  String indexedField;
  //boolean rotate;
  
  public void setUp() {
    advisor = new IndexAdvisor<String>(/*new StringMarDemar()*/);
    //rotate = false;
  }
  
  public void testInitialAdvise() {
    assertNull(advisor.fieldToIndex());
  }
  
  public void testNoFieldsInQuery() {
    // should handle this gracefully
    advisor.queryPerformed(new String[0], 1000000);
    assertNull(advisor.fieldToIndex());
  }
  
  public void testAdviseAfterQuery() {
    advisor.queryPerformed(new String[] { "age" }, 5000);
    assertEquals("age", advisor.fieldToIndex());
  }
  
  public void testFreeze() {
    advisor.freeze();
    advisor.queryPerformed(new String[] { "age" }, 5000);
    assertNull(advisor.fieldToIndex());
  }
  
  public void testNoImplicitHurdleChange() {
    advisor.queryPerformed(new String[] { "age" }, 5000);
    assertEquals("age", advisor.fieldToIndex());
    advisor.queryPerformed(new String[] { "name" }, 5001);
    assertEquals("name", advisor.fieldToIndex());
  }
  
  /** another field needs twice as many accesses before it becomes the index field */
  public void testAdviseChange() throws Exception {
    // query by age
    advisor.queryPerformed(new String[] { "age" }, 5000);
    assertEquals("age", advisor.fieldToIndex());
    
    rotate();
    
    // make age the indexed field
    acceptAdvise();
    
    // query by name
    advisor.queryPerformed(new String[] { "name" }, 9999);
    assertEquals("age", advisor.fieldToIndex());
    
    rotate();
    
    // query by name some more
    advisor.queryPerformed(new String[] { "name" }, 1);
    assertEquals("name", advisor.fieldToIndex());
    
    rotate();
    
    // make name the indexed field
    acceptAdvise();
    
    rotate();
    
    // query by age again
    advisor.queryPerformed(new String[] { "age" }, 14999);
    assertEquals("name", advisor.fieldToIndex());
    
    rotate();
    
    // query by age some more
    advisor.queryPerformed(new String[] { "age" }, 1);
    assertEquals("age", advisor.fieldToIndex());
  }
  
  /*public void testAdviseChangeRotate() throws Exception {
    rotate = true;
    testAdviseChange();
  }*/
  
  void acceptAdvise() {
    advisor.setIndexedField(indexedField = advisor.fieldToIndex());
  }
  
  void rotate() throws Exception {
    /*if (rotate) {
      MasterChunkManager cm = new DefaultChunkManager(new MemoryDisk(), DiskStore.VERSION);
      ChunkRef cr = advisor.saveStats(cm);
      
      // gc
      BitSet whiteList = new BitSet();
      advisor.collectChunks(whiteList, cr);
      cm.deleteEverythingBut(whiteList);
      
      advisor = new IndexAdvisor<String>(new StringMarDemar(), cm, cr);
      advisor.setIndexedField(indexedField);
    }*/
  }
}
