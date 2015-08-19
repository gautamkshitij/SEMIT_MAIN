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
import org.artsProject.mcop.*;

public class FastIntVector {
  int blockSize = 32;
  ArrayList<Block> blocks = new ArrayList<Block>();
  int size;
  int cacheIndex = -2, cacheI, cacheB;
  
  class Block {
    int[] data = new int[blockSize];
    int length = 0;
    
    void add(int i) {
      data[length++] = i;
    }
    
    void add(int index, int value) {
      System.arraycopy(data, index, data, index+1, length-index);
      data[index] = value;
      ++length;
    }
    
    int get(int index) {
      return data[index];
    }
    
    int[] data() {
      int[] result = new int[length];
      System.arraycopy(data, 0, result, 0, length);
      return result;
    }
    
    boolean full() {
      return length >= blockSize;
    }
    
    boolean overfull() {
      return length > blockSize;
    }
    
    boolean underfull() {
      return length < blockSize/2;
    }
    
    void remove(int index) {
      --length;
      System.arraycopy(data, index+1, data, index, length-index);
    }
    
    void set(int index, int value) {
      data[index] = value;
    }
    
    Block split() {
      Block b = new Block();
      int l = length/2;
      b.length = length-l;
      System.arraycopy(data, l, b.data, 0, b.length);
      length = l;
      
      // slim down overfat merged block
      if (data.length > blockSize) {
        int[] d = new int[blockSize];
        System.arraycopy(data, 0, d, 0, length);
        data = d;
      }
      
      return b;
    }
    
    void mergeWith(Block b) {
      int l = length+b.length;
      if (l > blockSize) {
        int[] d = new int[l];
        System.arraycopy(data, 0, d, 0, length);
        data = d;
      }
      System.arraycopy(b.data, 0, data, length, b.length);
      length = l;
    }
  }
  
  public FastIntVector(/*ChunkManager cm*/) {
    blocks.add(new Block());
  }
  
  public void read(Buffer b) {
    int n = b.readLong();
    for (int i = 0; i < n; i++)
      add(b.readLong());
  }
  
  public void marshal(Buffer buf) {
    buf.writeLong(size);
    for (int b = 0; b < blocks.size(); b++) {
      Block block = blocks.get(b);
      for (int i = 0; i < block.length; i++)
        buf.writeLong(block.data[i]);
    }
  }
  
  public void setBlockSize(int entries) {
    blockSize = entries;
  }
  
  public void add(int index, int value) {
    long n = findIndex(index);
    int b = (int) n, ofs = (int) (n >> 32);
    if (b == blocks.size()) {
      --b;
      ofs += blocks.get(blocks.size()-1).length;
    }
    Block block = blocks.get(b);
    if (block.full()) {
      Block newBlock = block.split();
      blocks.add(b+1, newBlock);
      if (ofs < block.length)
        block.add(ofs, value);
      else
        newBlock.add(ofs-block.length, value);
    } else
      block.add(ofs, value);
    ++size;
    clearCache();
  }
  
  public void set(int index, int value) {
    long n = findIndex(index);
    blocks.get((int) n).set((int) (n >> 32), value);
  }
  
  public void add(int i) {
    Block b = blocks.get(blocks.size()-1);
    if (b.full())
      blocks.add(b = new Block());
    b.add(i);
    ++size;
    clearCache();
  }
  
  /** returns block number | (offset &lt;&lt; 32) */
  long findIndex(int index) {
    // shortcut: end of vector
    if (index == size)
      return blocks.size();
      
    // hit on cached index
    if (index == cacheIndex)
      return cacheB | ((long) cacheI << 32);
      
    // hit on successor of cached index
    if (index == cacheIndex+1) {
      ++cacheIndex;
      if (++cacheI >= blocks.get(cacheB).length) {
        ++cacheB;
        cacheI = 0;
      }
      return cacheB | ((long) cacheI << 32);
    }
      
    int b, n = 0;
    for (b = 0; b < blocks.size(); b++) {
      int nextN = n+blocks.get(b).length;
      if (index < nextN) {
        cacheIndex = index;
        cacheB = b;
        cacheI = index-n;
        return cacheB | ((long) cacheI << 32);
      }
      n = nextN;
    }

    return b;
  }
  
  public int get(int index) {
    // what a shitty way to return two ints...
    long n = findIndex(index);
    return blocks.get((int) n).get((int) (n >> 32));
  }
  
  public int numBlocks() {
    return blocks.size();
  }
  
  public int[] getBlockData(int blockNr) {
    return blocks.get(blockNr).data();
  }
  
  public int indexOf(int value) {
    int n = 0;
    for (int b = 0; b < blocks.size(); b++) {
      Block block = blocks.get(b);
      int[] data = block.data;
      for (int i = 0; i < block.length; i++)
        if (data[i] == value)
          return n+i;
      n += block.length;
    }
    return -1;
  }
  
  public boolean removeElement(int value) {
    for (int b = 0; b < blocks.size(); b++) {
      Block block = blocks.get(b);
      int[] data = block.data;
      for (int i = 0; i < block.length; i++) {
        if (data[i] == value) {
          remove(b, i);
          return true;
        }
      }
    }
    return false;
  }
  
  void remove(int b, int i) {
    Block block = blocks.get(b);
    block.remove(i);
    --size;
    clearCache();
    
    // handle underflow
    if (block.underfull() && blocks.size() != 1) {
      if (b != 0) --b;
      block = blocks.get(b);
      block.mergeWith(blocks.get(b+1));
      blocks.remove(b+1);
      if (block.overfull())
        blocks.add(b+1, block.split());
    }
  }
  
  public void remove(int index) {
    long n = findIndex(index);
    remove((int) n, (int) (n >> 32));
  }
  
  public int size() {
    return size;
  }
  
  class I implements IntIterator {
    int b, i;
    Block block = blocks.get(0);
    
    public boolean hasNext() {
      return i < block.length;
    }
    
    public int next() {
      int result = block.get(i++);
      if (i >= block.length && b < blocks.size()-1) {
        block = blocks.get(++b);
        i = 0;
      }
      return result;
    }
  }
  
  public IntIterator iterator() {
    return new I();
  }
  
  public boolean isEmpty() {
    return size == 0;
  }
  
  public void clear() {
    blocks.clear();
    blocks.add(new Block());
    size = 0;
    clearCache();
  }
  
  void clearCache() {
    cacheIndex = -2;
  }
}
