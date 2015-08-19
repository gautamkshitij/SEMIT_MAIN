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

package drjava.smyle.core;

import java.util.*;

public class MemoryBTree<A,B> extends BTree<A,B> {
  final int m;
  NodeImpl root = new NodeImpl(true);
  Comparator<A> comparator;
  
  static final boolean debug = false;
  
  class SplitResult {
    A key;
    NodeImpl newNode;
    
    SplitResult(A key, NodeImpl newNode) {
      this.key = key;
      this.newNode = newNode;
    }
  }
  
  public class NodeImpl implements BTree<A,B>.Node {
    // leafs: n keys, n values
    // non-leafs: n-1 keys, n children
    ArrayList<A> keys;
    ArrayList<NodeImpl> children;
    ArrayList<B> values;
    
    public boolean isLeaf() {
      return children == null;
    }
    
    public int numChildren() {
      return children.size();
    }
    
    public int numValues() {
      return values.size();
    }
    
    public BTree<A,B>.Node child(int index) {
      return children.get(index);
    }
    
    public B value(int index) {
      return values.get(index);
    }
    
    public A key(int index) {
      return keys.get(index);
    }
    
    NodeImpl(boolean leaf) {
      keys = new ArrayList<A>();
      if (leaf)
        values = new ArrayList<B>();
      else
        children = new ArrayList<NodeImpl>();
    }
    
    /** returns true if an overflow occurred */
    boolean add(A key, B value) {
      if (isLeaf()) {
        int i = find(key);
        if (debug) System.out.println("Leaf: inserting "+key+" at "+i+" of "+keys.size());
        if (i >= 0) {
          // key exists, replace
          keys.set(i, key);
          values.set(i, value);
        } else {
          i = ~i;
          // new key, insert
          keys.add(i, key);
          values.add(i, value);
        }
        
        return numValues() > m;
      } else {
        int i = find(key);
        if (i < 0) i = ~i; else ++i;
        NodeImpl child = children.get(i);
        if (debug) System.out.println("Delegating "+key+" insertion to child "+i+" of "+children.size());
        if (child.add(key, value)) {
          // handle overflow
          SplitResult sr = child.split();
          children.add(i+1, sr.newNode);
          keys.add(i, sr.key);
        }
        
        return numChildren() > m;
      }
    }
      
    /** splits the node in half */
    SplitResult split() {
      if (isLeaf()) {
        NodeImpl newNode = new NodeImpl(true);
        int n = keys.size();
        int i = n/2;
        if (debug) System.out.println("Leaf splitting (m="+m+",n="+n+")");
        List<A> subKeys = keys.subList(i, n);
        List<B> subValues = values.subList(i, n);
        newNode.keys.addAll(subKeys);
        newNode.values.addAll(subValues);
        subKeys.clear();
        subValues.clear();
        if (debug) System.out.println("L first: "+values.get(0)+", R first: "+newNode.values.get(0));
        return new SplitResult(newNode.keys.get(0), newNode);
      } else {
        if (debug) System.out.println("Node splitting");
        NodeImpl newNode = new NodeImpl(false);
        int n = children.size();
        int i = n/2;
        A splitKey = keys.get(i-1);
        List<A> subKeys = keys.subList(i, n-1);
        List<NodeImpl> subChildren = children.subList(i, n);
        newNode.keys.addAll(subKeys);
        newNode.children.addAll(subChildren);
        keys.subList(i-1, n-1).clear();
        children.subList(i, n).clear();
        return new SplitResult(splitKey, newNode);
      }
    }

    /** returns a non-negative index if key was found;
        returns the one-complement of best-matching index otherwise */
    int find(A key) {
      int i = 0, c = -1;
      while (i < keys.size() && (c = comparator.compare(keys.get(i), key)) < 0)
        ++i;
      return c == 0 ? i : ~i;
    }
    
    /** returns true if an underflow occurred
       (also if node was already underfull on method entrance) */
    boolean remove(A key) {
      if (isLeaf()) {
        int i = find(key);
        if (i >= 0) {
          if (debug) System.out.println("Leaf: removing "+key+" at "+i+" of "+keys.size());
          keys.remove(i);
          values.remove(i);
        } else {
          i = ~i;
          if (debug) System.out.println("Leaf: "+key+" not found near "+i+" of "+keys.size());
        }
        
        return numValues() < (m+1)/2;
      } else {
        int i = find(key);
        if (i >= 0) ++i; else i = ~i;
        NodeImpl child = children.get(i);
        if (debug) System.out.println("Delegating "+key+" deletion to child "+i+" of "+children.size());
        if (child.remove(key)) {
          // handle underflow
          if (i != 0) --i;
          child = children.get(i);
          child.mergeWith(children.get(i+1), keys.get(i));
          children.remove(i+1);
          keys.remove(i);
          if (child.overfull()) {
            SplitResult sr = child.split();
            children.add(i+1, sr.newNode);
            keys.add(i, sr.key);
          }
        }
        
        return numChildren() < (m+1)/2;
      }
    }
    
    boolean overfull() {
      return isLeaf() ? numValues() > m : numChildren() > m;
    }
    
    void mergeWith(NodeImpl node, A middleKey) {
      if (isLeaf()) {
        keys.addAll(node.keys);
        values.addAll(node.values);
      } else {
        keys.add(middleKey);
        keys.addAll(node.keys);
        children.addAll(node.children);
      }
    }
  }
  
  /** @param m maximum number of children per node */
  public MemoryBTree(int m, Comparator<A> comparator) {
    this.m = m;
    this.comparator = comparator;
  }
  
  public BTree<A,B>.Node root() {
    return root;
  }
  
  public void put(A a, B b) {
    if (root.add(a, b)) {
      // handle overflow
      SplitResult sr = root.split();
      NodeImpl newRoot = new NodeImpl(false);
      newRoot.children.add(root);
      newRoot.children.add(sr.newNode);
      newRoot.keys.add(sr.key);
      root = newRoot;
    }
  }
  
  public void remove(A a) {
    if (root.remove(a)) {
      // only handle extreme underflow case: root only contains one child
      if (!root.isLeaf() && root.numChildren() == 1)
        root = root.children.get(0);
    }
  }
  
  public B get(A key) {
    NodeImpl node = root;
    
    while (!node.isLeaf()) {
      int i = node.find(key);
      if (i >= 0) ++i; else i = ~i;
      node = node.children.get(i);
    }
    
    int i = node.find(key);
    if (i >= 0) {
      return node.values.get(i);
    } else {
      return null;
    }
  }
  
  public int m() { return m; }
  
  public void clear() {
    root = new NodeImpl(true);
  }
}
