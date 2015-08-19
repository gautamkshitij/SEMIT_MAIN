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
import java.io.*;
import org.artsProject.mcop.*;
import drjava.smyle.*;

public final class PersistentBTree<A,B> extends BTree<A,B> {
  final int m;
  Handles<NodeImpl> handles;
  Handle<NodeImpl> root;
  Comparator<A> comparator;
  MarDemar<A> aMarDemar;
  MarDemar<B> bMarDemar;
  ChunkManager cm;
  ChunkRef handlesChunk, master;
  
  static final boolean debug = false;
  
  class SplitResult {
    A key;
    NodeImpl newNode;
    
    SplitResult(A key, NodeImpl newNode) {
      this.key = key;
      this.newNode = newNode;
    }
  }
  
  public class NodeImpl implements BTree<A,B>.Node, Handled<NodeImpl> {
    // leafs: n keys, n values
    // non-leafs: n-1 keys, n children
    ArrayList<A> keys;
    ArrayList<Handle<NodeImpl>> children;
    ArrayList<B> values;
    Handle<NodeImpl> handle;
    
    public void setHandle(Handle<NodeImpl> handle) {
      this.handle = handle;
    }
    
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
      return getChild(index);
    }
    
    NodeImpl getChild(int index) {
      return children.get(index).get();
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
        children = new ArrayList<Handle<NodeImpl>>();
    }
    
    /** returns true if an overflow occurred */
    boolean put(A key, B value) {
      int i = find(key);
      if (isLeaf()) {
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
        save();
        
        return numValues() > m;
      } else {
        if (i < 0) i = ~i; else ++i;
        NodeImpl child = getChild(i);
        if (debug) {
          System.out.println("Delegating "+key+" insertion to child "+i+" of "+children.size());
          if (i != 0)
            System.out.println("Left key: "+keys.get(i-1));
          if (i < keys.size())
            System.out.println("Right key: "+keys.get(i));
        }
        if (child.put(key, value)) {
          // handle overflow
          SplitResult sr = child.split();
          children.add(i+1, handles.add(sr.newNode));
          keys.add(i, sr.key);
          save();
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
        save();
        if (debug) System.out.println("L first: "+values.get(0)+", R first: "+newNode.values.get(0));
        return new SplitResult(newNode.keys.get(0), newNode);
      } else {
        if (debug) System.out.println("Node splitting");
        NodeImpl newNode = new NodeImpl(false);
        int n = children.size();
        int i = n/2;
        A splitKey = keys.get(i-1);
        List<A> subKeys = keys.subList(i, n-1);
        List<Handle<NodeImpl>> subChildren = children.subList(i, n);
        newNode.keys.addAll(subKeys);
        newNode.children.addAll(subChildren);
        keys.subList(i-1, n-1).clear();
        children.subList(i, n).clear();
        save();
        return new SplitResult(splitKey, newNode);
      }
    }
    
    int compare(A key1, A key2) {
      return comparator != null
        ? comparator.compare(key1, key2)
        : ((Comparable) key1).compareTo(key2);
    }

    /** returns a non-negative index if key was found;
        returns the one-complement of best-matching index otherwise */
    int find(A key) {
      /*int i = 0, c = -1;
      while (i < keys.size() && (c = compare(keys.get(i), key)) < 0)
        ++i;
      return c == 0 ? i : ~i;*/
      
      int low = 0;
      int high = keys.size()-1;
    
      while (low <= high) {
        int mid = (low+high)/2;
        int cmp = compare(keys.get(mid), key);
        
        if (cmp < 0)
          low = mid + 1;
        else if (cmp > 0)
          high = mid - 1;
        else
          return mid;
    	}
      
    	return ~low;
    }
    
    /** returns true if an underflow occurred
       (also if node was already underfull on method entrance) */
    boolean remove(A key) {
      int i = find(key);
      if (isLeaf()) {
        if (i >= 0) {
          if (debug) System.out.println("Leaf: removing "+key+" at "+i+" of "+keys.size());
          keys.remove(i);
          values.remove(i);
          save();
        } else {
          i = ~i;
          if (debug) System.out.println("Leaf: "+key+" not found near "+i+" of "+keys.size());
        }
        
        return numValues() < (m+1)/2;
      } else {
        if (i >= 0) ++i; else i = ~i;
        NodeImpl child = getChild(i);
        if (debug) System.out.println("Delegating "+key+" deletion to child "+i+" of "+children.size());
        if (child.remove(key)) {
          // handle underflow
          NodeImpl rChild;
          if (i != 0) {
            rChild = child;
            child = getChild(--i);
          } else
            rChild = getChild(i+1);
          child.mergeWith(rChild, keys.get(i));
          rChild.handle.dispose();
          children.remove(i+1);
          keys.remove(i);
          if (child.overfull()) {
            SplitResult sr = child.split();
            children.add(i+1, handles.add(sr.newNode));
            keys.add(i, sr.key);
          }
          save();
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
      save();
    }
    
    Handle save() {
      handle.invalidate();
      return handle;
    }
    
    void marshal(Buffer b) {
      b.writeBoolean(isLeaf());
      if (isLeaf()) {
        b.writeLong(values.size());
        for (int i = 0; i < values.size(); i++) {
          aMarDemar.marshal(b, keys.get(i));
          bMarDemar.marshal(b, values.get(i));
        }
      } else {
        b.writeLong(children.size());
        for (int i = 0; i < children.size(); i++) {
          if (i != 0)
            aMarDemar.marshal(b, keys.get(i-1));
          children.get(i).marshal(b);
        }
      }
    }
    
    NodeImpl(Buffer b) {
      keys = new ArrayList<A>();
      if (b.readBoolean()) {
        values = new ArrayList<B>();
        int n = b.readLong();
        for (int i = 0; i < n; i++) {
          keys.add(aMarDemar.read(b));
          values.add(bMarDemar.read(b));
        }
      } else {
        children = new ArrayList<Handle<NodeImpl>>();
        int n = b.readLong();
        for (int i = 0; i < n; i++) {
          if (i != 0)
            keys.add(aMarDemar.read(b));
          children.add(handles.read(b));
        }
      }
    }
  }
  
  class NodeMarDemar implements MarDemar<NodeImpl> {
    public void marshal(Buffer b, NodeImpl node) {
      node.marshal(b);
    }
    
    public NodeImpl read(Buffer b) {
      return new NodeImpl(b);
    }
  }
  
  /** @param m maximum number of children per node */
  public PersistentBTree(int m, Comparator<A> comparator, 
    MarDemar<A> aMarDemar, MarDemar<B> bMarDemar, ChunkManager cm,
    ChunkRef master) {
    this.m = m;
    this.comparator = comparator;
    this.aMarDemar = aMarDemar;
    this.bMarDemar = bMarDemar;
    this.cm = cm;
    this.master = master;

    if (master != null) {    
      Buffer b = cm.readChunk(master);
      handles = new Handles<NodeImpl>(cm, new NodeMarDemar(), new ChunkRef(b));
      root = handles.read(b);
    } else {
      handles = new Handles<NodeImpl>(cm, new NodeMarDemar());
      root = handles.add(new NodeImpl(true)); // gets handle index 0
    }
  }
  
  public ChunkRef flush() {
    ChunkRef chunk = handles.flush();
    if (!chunk.equals(handlesChunk)) {
      handlesChunk = chunk;
      Buffer b = new Buffer();
      chunk.writeType(b);
      root.marshal(b);
      master = cm.createChunk(b);
    }
    return master;
  }
  
  public BTree<A,B>.Node root() {
    return root.get();
  }
  
  public void put(A a, B b) {
    if (root.get().put(a, b)) {
      // handle overflow
      SplitResult sr = root.get().split();
      NodeImpl newRoot = new NodeImpl(false);
      newRoot.children.add(root);
      newRoot.children.add(handles.add(sr.newNode));
      newRoot.keys.add(sr.key);
      root = handles.add(newRoot);
    }
  }
  
  public void remove(A a) {
    if (root.get().remove(a)) {
      // only handle extreme underflow case: root only contains one child
      NodeImpl r = root.get();
      if (!r.isLeaf() && r.numChildren() == 1) {
        r.children.get(0).dispose();
        root.set(r.getChild(0));
      }
    }
  }
  
  public B get(A key) {
    NodeImpl node = root.get();
    
    while (!node.isLeaf()) {
      int i = node.find(key);
      if (i >= 0) ++i; else i = ~i;
      node = node.getChild(i);
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
    root.set(new NodeImpl(true));
  }
  
  public void collectChunks(BitSet whiteList) {
    handles.collectChunks(whiteList);
    if (master != null)
      whiteList.set(master.index);
  }
}
