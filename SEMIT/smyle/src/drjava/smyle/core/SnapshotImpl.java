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

import java.io.*;
import java.util.*;
import java.lang.Object;
import java.lang.ref.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import org.artsProject.util.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;

public final class SnapshotImpl implements Snapshot {
  DiskStore store;
  
  // while converting legacy stores, this is different from chunkManager
  ChunkManager newChunkManager;
  
  ChunkRef master = ChunkManager.NULLCHUNK;
  Buffer masterBuffer;
  ArrayList<String> tableNames = new ArrayList<String>();
  ArrayList<ChunkRef> tableChunks = new ArrayList<ChunkRef>();
  ArrayList<TableImpl> tables = new ArrayList<TableImpl>();
  int references = 0;
  boolean mutable;
  boolean unwritten;
  
  // debugging
  public int recCount;
  public Thread currentThread;
  public long workingSince;
  public String currentFunctions[];
  
  SnapshotImpl(DiskStore store, ChunkManager newChunkManager, boolean mutable) {
    if (DiskStore.debug) new Throwable(this.toString()).printStackTrace();
    this.store = store;
    this.newChunkManager = newChunkManager;
    this.mutable = mutable;
    master = store.chunkManager.getMasterChunk();
    //if (debug) System.out.println("Master="+master);
    loadMaster();
  }
  
  synchronized void assertSnapshotOpen() throws ClosedSnapshotException {
    if (references == 0)
      throw new ClosedSnapshotException("");
  }
  
  public synchronized <T extends Struct<T>> Table<T> getTable(String name,
  StructInfo<T> typeInfo) {
    assertSnapshotOpen();
    store.assertOpen();
    
    int i = tableNames.indexOf(name);
    if (i < 0) {
      i = tableNames.size();
      //if (debug) System.out.println("New table "+name+" ("+i+" tables exist)");
      tableNames.add(name);
      tableChunks.add(ChunkManager.NULLCHUNK);
      tables.add(null);
    }
    TableImpl table = tables.get(i);
    if (table == null || !table.typed())
      tables.set(i, table = new TableImpl<T>(this, typeInfo, i, tableChunks.get(i), name));
    return table; // compiler warning ok
  }
  
  public synchronized void commitAndContinue() throws SmyleIOException, ConcurrentWriteException {
    assertSnapshotOpen();
    store.assertOpen();
    
    if (store.deferThisCommit()) {
      unwritten = true;
      store.scheduleFlush();
    } else {
      save();
    }
  }

  synchronized void save() {
    for (int i = 0; i < tables.size(); i++) {
      TableImpl table = tables.get(i);
      if (table != null) {
        table.buildIndexIfAdvised();
        table.flush();
      }
    }
        
    //System.out.println("commitAndContinue: tables: "+tables.size()+", changed="+(masterBuffer != null));
    if (masterBuffer != null) {
      synchronized(store) {
        // The situation has changed since our snapshot was taken
        // Just fail, even though there might be no conflict
        if (!store.chunkManager.getMasterChunk().equals(master)) {
          if (store.logger != null && store.masterWriteSite != null)
            store.masterWriteSite.printStackTrace(store.logger);
          throw new ConcurrentWriteException("");
        }
          
        master = store.saveMaster(masterBuffer);
        //if (DiskStore.debug) new Throwable("Writing "+masterBuffer.remaining()+" bytes to master "+master.index).printStackTrace();
        masterBuffer = null;
        
        /*if (tables.size() != 0)
          if (debug) System.out.println("Saving master "+master+", table 0 elementsChunk: "+tableChunks.get(0));*/
          
      }
      store.maybeGC();
    }
  }
  
  public synchronized void commit() throws SmyleIOException, ConcurrentWriteException {
    commitAndContinue();
    forget();
  }
  
  synchronized void collectChunks(BitSet whiteList) {
    //if (debug) System.out.println("Adding master "+master+" to whitelist");
    
    // exit if snapshot has already been processed
    if (whiteList.get(master.index))
      return;
      
    whiteList.set(master.index);
    for (int i = 0; i < tableChunks.size(); i++)
      whiteList.set(tableChunks.get(i).index);
    for (int i = 0; i < tables.size(); i++)
      getOrCreateTable(i).collectChunks(whiteList);
  }
  
  synchronized void saveMaster() {
    Buffer b = new Buffer();
    b.writeLong(store.VERSION);
    MCOP.writeSeq(b, tableChunks);
    MCOP.writeSeq(b, tableNames);
    masterBuffer = b;
  }
  
  private void loadMaster() {
    try {
      if (master.index != 0) {
        //if (DiskStore.debug) new Throwable("Loading master chunk "+master.index).printStackTrace();
        Buffer b = store.chunkManager.readChunk(master);
        
        // read and check version
        
        store.version = b.readLong();
        if (store.version > store.VERSION)
          throw new BadUseException("This version of Smyle is too old to operate this store, please upgrade (this is revision "+store.VERSION+", you need at least "+store.version+")");
          
        //if (debug) System.out.println("Master id: "+master+", bytes: "+b.remaining());
        MCOP.readSeq(b, tableChunks, ChunkRef.DEMARSHALLER);
        MCOP.readSeq(b, tableNames);
        if (tableChunks.size() != tableNames.size())
          throw new InternalSmyleError("Database is corrupt");
        for (int i = 0; i < tableChunks.size(); i++)
          tables.add(null);
          
        if (store.version < store.VERSION) {
          // load all tables with store version
          for (int i = 0; i < tableNames.size(); i++) {
            tables.set(i,
              new TableImpl(this, i, tableChunks.get(i), tableNames.get(i)));
          }
            
          // bump version and store tables
          store.version = store.VERSION;
          for (int i = 0; i < tables.size(); i++) {
            TableImpl t = tables.get(i);
      	    t.saveSchema();
      	    t.dirty = true;
            t.flush();
            //if (DiskStore.debug) System.out.println("Table chunk: "+tableChunks.get(i));
          }
          //commitAndContinue();
        }
      }
    } catch (MCOPException e) {
      throw new InternalSmyleError(e);
    }
  }
  
  synchronized void setTableChunk(int tableId, ChunkRef file) {
    //if (debug) System.out.println("setTableChunk "+tableId+" "+file.id);
    tableChunks.set(tableId, file);
    saveMaster();
  }
  
  public synchronized void forget() {
    if (references == 0) return;
    --references;
    if (references == 0)
      synchronized(store) {
        if (mutable) {
          //if (store.writeSnapshot == this)
          store.releaseWriteLock();
        }
           
        store.forgetSnapshot(this);
      }
  }

  /** re-store a chunk in newChunkManager */
  synchronized ChunkRef reStoreChunk(ChunkRef chunk) {
    if (chunk.index == 0) return chunk;
    return newChunkManager.createChunk(store.chunkManager.readChunk(chunk));
  }

  synchronized void assertMutable() {    
    if (!mutable)
      throw new ImmutableSnapshotException("");
  }
  
  public synchronized SortedSet<String> getTableNames() {
    return new TreeSet<String>(tableNames);
  }
  
  public synchronized UntypedTable getUntypedTable(String name) {
    assertSnapshotOpen();
    store.assertOpen();
    
    int i = tableNames.indexOf(name);
    if (i < 0) return null;
    return getOrCreateTable(i);
  }
  
  private TableImpl getOrCreateTable(int i) {
    TableImpl table = tables.get(i);
    if (table == null)
      tables.set(i, table = new TableImpl(this, i, tableChunks.get(i), tableNames.get(i)));
    return table;
  }
  
  public synchronized boolean equals(Object o) {
    return o instanceof SnapshotImpl
      && master.equals(((SnapshotImpl) o).master);
  }
  
  /*synchronized void ref() {
    ++references;
  }*/
}

