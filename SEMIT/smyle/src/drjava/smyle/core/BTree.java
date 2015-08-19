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
import drjava.smyle.*;

public abstract class BTree<A,B> {
  public interface Node {
    public boolean isLeaf();
    public int numChildren();
    public int numValues();
    public Node child(int index);
    public B value(int index);
    public A key(int index);
  }
  
  public abstract Node root();
  public abstract void put(A a, B b);
  public abstract void remove(A a);
  public abstract B get(A a);
  public abstract int m();
  public abstract void clear();
  
  public MapIterator<A,B> iterate() {
    return iterate(false);
  }
  
  class PathEntry {
    Node node;
    boolean reverse;
    int index, highest;
    
    PathEntry(Node node, boolean reverse) {
      this.node = node;
      this.reverse = reverse;
      highest = node.isLeaf() ? node.numValues() : node.numChildren();
      if (reverse)
        index = highest-1;
    }
    
    boolean exhausted() {
      return index < 0 || index >= highest;
    }
    
    int getIndexAndSkip() {
      int result = index;
      if (reverse) --index; else ++index;
      return result;
    }
  }

  public MapIterator<A,B> iterate(final boolean reverse) {
    class I implements MapIterator<A,B> {
      Stack<PathEntry> stack = new Stack<PathEntry>();
      A key;
      
      I() {
        stack.push(new PathEntry(root(), reverse));
        findNext();
      }
      
      private void checkForComodification() {
      }
      
      public boolean hasNext() {
        checkForComodification();
        return !stack.isEmpty();
      }
      
      public B next() {
        checkForComodification();
        PathEntry e = stack.peek();
        int i = e.getIndexAndSkip();
        B result = e.node.value(i);
        key = e.node.key(i);
        findNext();
        return result;
      }
      
      public void remove() {
        throw new RuntimeException("TODO");
      }
      
      void findNext() {
        // Pop completed nodes
        while (!stack.isEmpty() && stack.peek().exhausted()) {
          stack.pop();
        }
        
        // Descend to leaf
        if (!stack.isEmpty()) {
          while (!stack.peek().node.isLeaf()) {
            stack.push(new PathEntry(stack.peek().node.child(stack.peek().getIndexAndSkip()), reverse));
          }
        }
      }

      public A getKey() {
        return key;
      }
    }
    return new I();
  }

  class MinMaxPathEntry extends PathEntry {
    KeySet<A> set;
    A min, max;

    MinMaxPathEntry(Node node, boolean reverse, KeySet<A> set, A min, A max) {
      super(node, reverse);
      this.set = set;
      this.min = min;
      this.max = max;
    }

    MinMaxPathEntry getEntryAndSkip() {
      int i = getIndexAndSkip();
      return new MinMaxPathEntry(node.child(i), reverse, set,
        i == 0 ? min : node.key(i-1),
        i == highest-1 ? max : node.key(i));
    }

    boolean interesting() { 
      return set.overlapsWithRange(min, max);
    }
  }
    
  public MapIterator<A,B> iterate(final A min, final A max, final KeySet<A> set, final boolean reverse) {
    class I implements MapIterator<A,B> {
      Stack<MinMaxPathEntry> stack = new Stack<MinMaxPathEntry>();
      A key;
      
      I() {
        stack.push(new MinMaxPathEntry(root(), reverse, set, min, max));
        findNext();
      }
      
      private void checkForComodification() {
      }
      
      public boolean hasNext() {
        checkForComodification();
        return !stack.isEmpty();
      }
      
      public B next() {
        checkForComodification();
        PathEntry e = stack.peek();
        int i = e.getIndexAndSkip();
        B result = e.node.value(i);
        key = e.node.key(i);
        findNext();
        return result;
      }
      
      public void remove() {
        throw new RuntimeException("TODO");
      }
      
      void findNext() {
        if (stack.isEmpty()) return;
        while (true) {
          while (!stack.peek().node.isLeaf() || stack.peek().exhausted()) {
            // Pop completed nodes
            while (stack.peek().exhausted()) {
              stack.pop();
              if (stack.isEmpty()) return;
            }

            // Descend one step if entry is interesting
            MinMaxPathEntry entry = stack.peek().getEntryAndSkip();
            if (entry.interesting()) stack.push(entry);
          }

          if (!set.contains(stack.peek().node.key(stack.peek().index)))
            stack.peek().getIndexAndSkip();
          else
            break;
        }
      }

      public A getKey() {
        return key;
      }
    }
    return new I();
  }
}
