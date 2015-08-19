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
import java.io.*;
import junit.framework.*;
import drjava.smyle.core.*;
import drjava.smyle.testtypes.*;

public abstract class BTreeTestBase extends TestCase {
  public BTreeTestBase(String name) { super(name); }
  
  BTree<String,String> tree;
  int m;
  final int n = 75, smallN = 25;
  TreeMap<String,String> map;
  int counter;
  
  Comparator<String> comparator = new Comparator<String>() {
    public int compare(String a, String b) {
      return Integer.parseInt(a)-Integer.parseInt(b);
    }
  };
  
  protected abstract BTree<String,String> createTree() throws Exception;
  
  protected void additionalChecks() throws Exception {
    checkGet();
    checkIterate();
  }
  
  void checkGet() throws Exception {
    for (int i = 0; i < n; i++) {
      String key = String.valueOf(i);
      assertEquals(map.get(key), tree.get(key));
    }
    assertNull(tree.get("-10"));
  }
  
  void checkIterate() throws Exception {
    // test iterate and build temp vector
    
    Iterator<String> i1 = map.values().iterator();
    Iterator<String> i2 = tree.iterate();
    ArrayList<String> v = new ArrayList<String>();
    while (i1.hasNext()) {
      assertTrue(i1.hasNext());
      String s = i1.next();
      assertEquals(s, i2.next());
      v.add(s);
    }
    assertTrue(!i2.hasNext());
    
    // test iterate(true) using temp vector
    
    i2 = tree.iterate(true);
    for (int i = v.size()-1; i >= 0; i--) {
      assertTrue(i2.hasNext());
      assertEquals(v.get(i), i2.next());
    }
    assertTrue(!i2.hasNext());
  }
  
  void checkIterateKeySet(final String s, String value) throws Exception {
    Iterator<String> i = tree.iterate(null, null, new KeySet<String>() {
      public boolean contains(String key) { 
        return key.equals(s); 
      }

      public boolean overlapsWithRange(String min, String max) {
        //System.out.println("overlapsWithRange "+min+" "+max);
	return (min == null || comparator.compare(min, s) <= 0)
	  &&   (max == null || comparator.compare(s, max) <  0);
      }
    }, false);
    assertTrue(i.hasNext());
    assertEquals(value, i.next());
    assertTrue(!i.hasNext());
  }

  public void setUp() throws Exception {
    m = 3;
    tree = createTree();
    assertEquals(m, tree.m());
    map = new TreeMap<String,String>(comparator);
    counter = 0;
  }
  
  public void testEmptyTree() throws Exception {
    assertTreeContents(map);
  }
  
  void put(String key, String value) throws Exception {
    tree.put(key, value);
    map.put(key, value);
    if ((counter++ & 1) == 0) assertTreeContents(map);
  }
  
  void remove(String key) throws Exception {
    tree.remove(key);
    map.remove(key);
    assertTreeContents(map);
  }
  
  public void testReverseInserts() throws Exception {
    for (int i = smallN; i > 0; i--)
      put(String.valueOf(i), "v"+i);
    additionalChecks();
    for (int i = smallN; i > 0; i--)
      remove(String.valueOf(i));
  }
  
  public void testLinearInserts() throws Exception {
    for (int i = 0; i < smallN; i++)
      put(String.valueOf(i), "v"+i);
    additionalChecks();
    for (int i = 0; i < smallN; i++)
      remove(String.valueOf(i));
  }
  
  public void testRandomInserts() throws Exception {
    for (int i = 0; i < 128; i++) {
      int j = i^0x56;
      put(String.valueOf(j), "v"+j);
    }
    additionalChecks();
    for (int i = 0; i < 128; i++) {
      int j = i^0x2d;
      remove(String.valueOf(j));
    }
  }
  
  public void testRandomInsertsM4() throws Exception {
    m = 4;
    tree = createTree();
    testRandomInserts();
  }
  
  public void testIterateKeySet() throws Exception {
    for (int i = 0; i < n; i++)
      put(String.valueOf(i), "v"+i);
    checkIterateKeySet("10", "v10");
  }

  static <A,B> void printTree(BTree<A,B> tree) throws Exception {
    StringBuffer buf = new StringBuffer();
    printSubTree(buf, (BTree.Node) tree.root());
    System.out.println("Tree: "+buf);
  }
  
  static <A,B> void printSubTree(StringBuffer buf, BTree<A,B>.Node node) throws Exception {
    buf.append('(');
    if (node.isLeaf()) {
      for (int i = 0; i < node.numValues(); i++) {
        if (i > 0)
          buf.append(',');
        buf.append(node.key(i)).append("->").append(node.value(i));
      }
    } else {
      for (int i = 0; i < node.numChildren(); i++) {
        if (i > 0)
          buf.append(',').append(node.key(i-1)).append(',');
        printSubTree(buf, (BTree.Node) node.child(i));
      }
    }
    buf.append(')');
  }
  
  /** also asserts the tree is balanced */
  void assertTreeContents(TreeMap<String,String> map) throws Exception {
    assertTreeContents(tree, map);
  }
  
  public static <A,B> void assertTreeContents(BTree<A,B> tree, TreeMap<A,B> map) throws Exception {
    try {
      Iterator<Map.Entry<A,B>> i = map.entrySet().iterator();
      assertNodeContents(tree, (BTree.Node) tree.root(), i);
      if (i.hasNext())
        fail("missing element at end: "+i.next());
    } catch (AssertionFailedError e) {
      printTree(tree);
      throw e;
    }
  }
  
  static <A,B> void assertNodeContents(BTree<A,B> tree, BTree<A,B>.Node node, Iterator<Map.Entry<A,B>> iterator) throws Exception {
    int m = tree.m();
    if (node.isLeaf()) {
      int n = node.numValues();
      if (node != tree.root()) {
        assert("Too few values: "+n+" (m="+m+")", n >= (m+1)/2);
      }
      assert("Too many values: "+n+" (m="+m+")", n <= m);
      for (int i = 0; i < node.numValues(); i++) {
        if (!iterator.hasNext())
          fail("Superfluous element: "+node.value(i)+" at "+i);
        assertEquals(iterator.next().getValue(), node.value(i));
      }
    } else {
      int n = node.numChildren();
      if (node == tree.root()) {
        assert("Too few root children: "+n, n >= 2);
      } else {
        assert("Too few children: "+n+" (m="+m+")", n >= (m+1)/2);
      }
      assert("Too many children: "+n+" (m="+m+")", n <= m);
      for (int i = 0; i < n; i++)
        assertNodeContents(tree, node.child(i), iterator);
    }
  }
  
  public static <A,B> Map<A,B> treeToMap(BTree<A,B> tree) {
    TreeMap<A,B> map = new TreeMap<A,B>();
    nodeToMap(tree.root(), map);
    return map;
  }
  
  public static <A,B> Map<A,B> treeToMap(BTree<A,B> tree, Comparator<A> comparator) {
    TreeMap<A,B> map = new TreeMap<A,B>(comparator);
    nodeToMap(tree.root(), map);
    return map;
  }
  
  static <A,B> void nodeToMap(BTree<A,B>.Node node, Map<A,B> map) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.numValues(); i++)
        map.put(node.key(i), node.value(i));
    } else {
      for (int i = 0; i < node.numChildren(); i++)
        nodeToMap(node.child(i), map);
    }
  }
  
  public void testReplaceValue() throws Exception {
    put("1", "1");
    put("1", "2");
  }
  
  public void testClear() throws Exception {
    put("1", "5");
    tree.clear();
    map.clear();
    assertTreeContents(map);
  }
  
  public void testDelegationBug() throws Exception {
    for (int i = 0; i < 4; i++)
      put(String.valueOf(i), String.valueOf(i));
    put("2","3");
  }
}
