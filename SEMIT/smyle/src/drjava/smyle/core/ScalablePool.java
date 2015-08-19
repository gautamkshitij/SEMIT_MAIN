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
import com.go.trove.util.*;
import drjava.smyle.*;

/*
Requirements:
-little memory consumption on startup
-efficient storage (more than one entry per chunk)
-quick access to any index
-quick adding (finding vacancy)
-individual changes should not invalidate large data structures

Architecture:
-linear list of blocks with 32 entries each

In memory:
-chunkref array, one for every block
-vacancy bit array (one bit per block)
-changed blocks are held in HashMap 
=> approx. 33 bits per entry

On disk:
-one chunk per block
-master chunk contains vacancy and chunkref arrays
*/

public final class ScalablePool<A> {
  static final int CLEANCACHE_MINSIZE = 64;

  // block sources
  static final int
    DIRTY   = 0,
    CLEAN   = 1,
    DISK    = 2,
    EMPTY   = 3,
    UNKNOWN = 4;

  BitSet vacancy = new BitSet();
  FastIntVector blocks = new FastIntVector();
  int firstVacancy;
  MarDemar<A> marDemar;
  ChunkManager cm;
  ChunkRef master;
  Cache<Integer,Block>   cleanCache = new Cache<Integer,Block>(CLEANCACHE_MINSIZE);
  HashMap<Integer,Block> dirtyCache = new HashMap<Integer,Block>();
  int blockSource; // temp var
  final Block emptyBlock = new Block(); // dummy entry for empty blocks in dirtyCache

  // stats
  int chunksCreated, dirtyCacheHit, cleanCacheHit, cacheMiss, emptyHit;
  
  final class Block implements Marshallable {
    A[] data = new A[32];
    int elements;
    
    boolean empty() {
      return elements == 0;
    }
    
    boolean vacancies() {
      return elements < 32;
    }
    
    int insert(A a) {
      for (int i = elements; i < elements+32; i++)
        if (data[i & 31] == null) {
          ++elements;
          data[i & 31] = a;
          return i & 31;
        }
      throw new RuntimeException("No vacancy");
    }
    
    void release(int i) {
      data[i] = null;
      --elements;
    }
    
    void replace(int i, A a) {
      data[i] = a;
    }
    
    public void marshal(Buffer b) {
      int pattern = 0;
      for (int i = 0; i < 32; i++)
        if (data[i] != null)
          pattern |= 1 << i;
      b.writeLong(pattern);
      for (int i = 0; i < 32; i++)
        if (data[i] != null)
          marDemar.marshal(b, data[i]);
    }
    
    Block() {
    }
    
    Block(Buffer b) {
      int pattern = b.readLong();
      for (int i = 0; i < 32; i++)
        if ((pattern & (1 << i)) != 0) {
          data[i] = marDemar.read(b);
          ++elements;
        }
    }
  }
  
  class BlockMarDemar implements MarDemar<Block> {
    public void marshal(Buffer b, Block block) {
      block.marshal(b);
    }
    
    public Block read(Buffer b) {
      return new Block(b);
    }
  }
  
  public ScalablePool(MarDemar<A> marDemar, ChunkManager cm, ChunkRef master) {
    this.marDemar = marDemar;
    this.cm = cm;
    if (master != null)
      loadMaster(master);
  }
  
  public int insert(A a) {
    master = null;
    for (int b = firstVacancy; b < blocks.size(); b++) {
      if (!vacancy.get(b)) continue;
      Integer B = new Integer(b);
      Block block = getBlock(B);
      if (block == null)
        block = new Block();
      if (!block.vacancies())
        throw new InternalSmyleError("Block "+b+" in vacancy list, but not vacant");
      int ofs = block.insert(a);
      setBlock(B, block, blockSource);
      if (!block.vacancies()) {
        vacancy.clear(b);
        firstVacancy = b+1;
      } else
        firstVacancy = b;
      //System.out.println("Elements in block "+b+" now: "+block.elements+" (vacancy: "+vacancy.get(b)+")");
      return (b << 5)+ofs;
    }
    Block block = new Block();
    int ofs = block.insert(a);
    int n = blocks.size();
    blocks.add(0);
    vacancy.set(n);
    setBlock(n, block);
    return (n << 5)+ofs;
  }
  
  public void replace(int i, A a) {
    while ((i >> 5) >= blocks.size())
      blocks.add(0);
    Block b = getBlock(i >> 5);
    if (b == null) b = new Block();
    b.replace(i & 31, a);
    setBlock(i >> 5, b);
    master = null;
  }
  
  private void setBlock(int i, Block b) {
    setBlock(new Integer(i), b, UNKNOWN);
  }

  private void setBlock(Integer I, Block b, int blockSource) {
    if (blockSource == UNKNOWN || blockSource == CLEAN) cleanCache.remove(I);
    if (blockSource != DIRTY) dirtyCache.put(I, b == null ? emptyBlock : b);
  }
  
  private void flushCache() {
    for (Iterator<Map.Entry<Integer,Block>> i = dirtyCache.entrySet().iterator();
      i.hasNext(); ) {
      Map.Entry<Integer,Block> e = i.next();
      int index = e.getKey().intValue();
      Block block = e.getValue();
      if (block == emptyBlock)
        blocks.set(index, 0);
      else {
        blocks.set(index, cm.createChunk(block).index);
        chunksCreated++;
      }
    }
    dirtyCache.clear();
  }
  
  public A get(int i) {
    if (i < 0 || i >= blocks.size()*32) return null;
    Block block = getBlock(i >> 5);
    return block == null ? null : block.data[i & 31];
  }
  
  public void release(int i) {
    int b = i >> 5;
    Block block = getBlock(b);
    block.release(i & 31);
    vacancy.set(b);
    if (b < firstVacancy) firstVacancy = b;
    if (block.empty()) {
      flushCache();
      blocks.set(b, 0);
      while (blocks.size() != 0 && blocks.get(blocks.size()-1) == 0)
        blocks.remove(blocks.size()-1);
    } else
      setBlock(b, block);
    master = null;
  }
  
  private void loadMaster(ChunkRef master) {
    Buffer b = cm.readChunk(master);
    blocks.read(b);
    for (int i = 0; i < blocks.size(); i += 32) {
      int val = b.readLong();
      for (int j = 0; j < 32; j++)
        if ((val & (1 << j)) != 0)
          vacancy.set(i+j);
    }
    this.master = master;
  }
  
  public ChunkRef flush() {
    if (master == null) {
      flushCache();
      Buffer b = new Buffer();
      blocks.marshal(b);
      for (int i = 0; i < blocks.size(); i += 32) {
        int val = 0;
        for (int j = 0; j < 32; j++)
          if (vacancy.get(i+j))
            val |= 1 << j;
        b.writeLong(val);
      }
      master = cm.createChunk(b);
      /*System.out.println("ScalablePool: created "+chunksCreated+" chunks, dirty/clean/miss/empty: "
        +dirtyCacheHit+"/"+cleanCacheHit+"/"+cacheMiss+"/"+emptyHit);*/
      chunksCreated = dirtyCacheHit = cleanCacheHit = cacheMiss = 0;
    }
    return master;
  }
  
  private Block getBlock(int i) {
    return getBlock(new Integer(i));
  }

  private Block getBlock(Integer I) {
    //new Throwable().printStackTrace();
    Block b = dirtyCache.get(I);
    if (b != null) {
      dirtyCacheHit++;
      blockSource = DIRTY;
      return b == emptyBlock ? null : b;
    }

    int chunk = blocks.get(I.intValue());
    if (chunk == 0) {
      emptyHit++;
      blockSource = EMPTY;
      return null;
    }

    b = cleanCache.get(I);
    if (b != null) {
      cleanCacheHit++;
      blockSource = CLEAN;
      return b;
    }
    
    b = new Block(cm.readChunk(chunk));
    cleanCache.put(I, b);
    cacheMiss++;
    blockSource = DISK;
    //if (cacheBlock != null) System.out.println("ScalablePool: loaded block "+i);
    return b;
  }
  
  public int size() {
    return blocks.size()*32;
  }
  
  public void collectChunks(BitSet whiteList) {
    whiteList.set(master.index);
    for (int i = 0; i < blocks.size(); i++)
      whiteList.set(blocks.get(i));
  }

  public void checkConsistency() {
    for (int i = 0; i < blocks.size(); i++) {
      if (vacancy.get(i) != getBlock(i).vacancies())
        throw new RuntimeException("Inconsistent at block "+i);
      if (vacancy.get(i) && i < firstVacancy)
        throw new RuntimeException("firstVacancy too high");
    }
  }
}
