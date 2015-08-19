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
import com.go.trove.util.*;
import org.artsProject.mcop.*;
import drjava.smyle.*;

/** read-only ChunkManager for converting legacy Smyle 0.2.5/0.3 beta databases */
public class ChunkManager025 extends MasterChunkManager {
  static final int
    DEFAULT_NATURALFILESIZE = 64*1024,
    MAXCACHESIZE = DEFAULT_NATURALFILESIZE;
    
  static final boolean debug = false;
    
  Disk disk;
  ArrayList<Chunk> chunks = new ArrayList<Chunk>();
  boolean masterLoaded = false;
  ChunkRef masterChunk = NULLCHUNK;
  Cache<Long,byte[]> fileCache = new Cache<Long,byte[]>(1);
  
  public ChunkManager025(Disk disk) {
    this.disk = disk;
  }
  
  // accessors
  
  public synchronized Buffer readChunk(int index) {
    try {
      if (index <= 0)
        throw new IllegalArgumentException("Invalid ChunkRef (index="+index+")");
      Chunk chunk = chunks.get(index-1);
      if (chunk.file == 0) {
        throw new InternalSmyleError("Chunk "+index+" doesn't exist anymore");
      } else {
        // look in cache
        Long lFile = new Long(chunk.file);
        byte[] data = fileCache.get(lFile);
        if (data != null)
          return new Buffer(data, chunk.offset, chunk.length);
        
        // read chunk from file
        DataInputStream in = new DataInputStream(disk.readFile(chunk.file));
        
        // store in cache if cacheable
        int fileLen = disk.getFileLength(chunk.file);
        if (fileLen <= MAXCACHESIZE) {
          data = new byte[fileLen];
          in.readFully(data);
          in.close();
          fileCache.put(lFile, data);
          return new Buffer(data, chunk.offset, chunk.length);
        } else {
          // Not cacheable
          in.skip(chunk.offset);
          data = new byte[chunk.length];
          in.readFully(data);
          in.close();
          return new Buffer(data);
        }
      }
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  public synchronized ChunkRef getMasterChunk() {
    if (!masterLoaded) {
      masterLoaded = true;
      loadMaster();
    }
    return masterChunk;
  }
  
  // mutators
  
  public synchronized ChunkRef createChunk(Buffer data) {
    throw new RuntimeException("This is a legacy chunk manager; read only");
  }
  
  void loadMaster() {
    long master = disk.getMasterFile();
    if (master != 0) {
      Buffer b = DiskUtil.fileToBuffer(disk, master);
      b.readLong(); // version
      chunks.clear();
      MCOP.readSeq(b, chunks, Chunk.DEMARSHALLER);
      masterChunk = new ChunkRef(b);
    }
  }
  
  public synchronized ChunkRef createMasterChunk(Buffer data) {
    throw new RuntimeException("This is a legacy chunk manager; read only");
  }
  
  public synchronized void deleteEverythingBut(BitSet whiteList) {
    throw new RuntimeException("This is a legacy chunk manager; read only");
  }
}

