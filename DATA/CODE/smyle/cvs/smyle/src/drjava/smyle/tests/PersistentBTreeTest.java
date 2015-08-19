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
import drjava.smyle.testtypes.*;

public class PersistentBTreeTest extends BTreeTestBase {
  public PersistentBTreeTest(String name) { super(name); }
  
  DefaultChunkManager cm;
  PersistentBTree<String,String> pTree;
  
  protected BTree<String,String> createTree() throws Exception {
    cm = new DefaultChunkManager(new MemoryDisk(), DiskStore.VERSION);
    pTree = new PersistentBTree<String,String>
      (m, comparator, new StringMarDemar(), new StringMarDemar(), cm, null);
    return pTree;
  }
  
  protected void additionalChecks() throws Exception {
    super.additionalChecks();
    ChunkRef master = pTree.flush();
    tree = pTree = new PersistentBTree<String,String>
      (m, comparator, new StringMarDemar(), new StringMarDemar(), cm, master);
  }
  
  int numChunks() throws Exception {
    pTree.flush();
    BitSet whiteList = new BitSet();
    pTree.collectChunks(whiteList);
    cm.deleteEverythingBut(whiteList);
    /*int n = 0;
    for (int i = 0; i < whiteList.size(); i++)
      if (whiteList.get(i)) ++n;
    System.out.println("White list entries: "+n);*/
    return cm.numChunks();
  }
  
  public void testRandomInserts() throws Exception {
    int chunks = numChunks();
    super.testRandomInserts();
    assertEquals("Number of chunks", chunks, numChunks());
  }
}
