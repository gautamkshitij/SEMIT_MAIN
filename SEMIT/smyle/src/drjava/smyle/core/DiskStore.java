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
import drjava.gjutil.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;
import drjava.smyle.core.indexing.*;

/** The primary implementation of Store. You don't need to reference this class directly
    in programs; use the static methods of class Smyle instead. */
    
// IMPORTANT RULE: Don't flush when synchronized on the store!
// TODO: follow the rule
public class DiskStore implements Store {
  Disk disk;
  MasterChunkManager chunkManager;
  DefaultChunkManager writeChunkManager;
  ArrayList<WeakReference<SnapshotImpl>> activeSnapshots = new ArrayList<WeakReference<SnapshotImpl>>();
  int version = VERSION;
  int references = 1;
  int timeout = 0;
  Throwable writeLock;
  SnapshotImpl writeSnapshot;
  int waiters = 0;
  Thread writingThread;
  PrintWriter logger = new PrintWriter(System.err, true);
  long gcFrequency = DEFAULT_GC_FREQUENCY;
  long lastGC;
  HashMap<String,IndexAdvisor<Pair<IndexProfile,Function>>> indexAdvisors = 
    new HashMap<String,IndexAdvisor<Pair<IndexProfile,Function>>>();
  SnapshotImpl cachedSnapshot; // an up-to-date immutable snapshot
  SnapshotImpl unwrittenSnapshot; // modified snapshot that needs to be saved
  int writeLatency = 0;
  Thread flusher;
  boolean deferNextCommit = writeLatency != 0;
  boolean readOnly;

  public static boolean
    debug = false, showGC = true;
  public static final int VERSION = 890;
  public static int DEFAULT_GC_FREQUENCY = 100000;
  static final boolean exclusiveWriteLocking = true; // write locking policy
  
  Throwable masterWriteSite;
  
  public DiskStore(File dir) {
    this(new FileSystemDisk(dir, false));
  }
  
  public DiskStore(Disk disk) {
    this(disk, false);
  }

  public DiskStore(Disk disk, boolean readOnly) {
    try {
      if (debug) System.out.println("DiskStore init");
      this.disk = disk;
      this.readOnly = readOnly;
      
      chunkManager = writeChunkManager = new DefaultChunkManager(disk, VERSION);
      
      // retrieve store version
      long master = disk.getMasterFile();
      if (master != 0) {
        DataInputStream dis = new DataInputStream(disk.readFile(master));
        byte[] data = new byte[4];
        dis.readFully(data);
        dis.close();
        version = new Buffer(data).readLong();
        
        if (debug) System.out.println("master file="+master+", version="+version+" (VERSION="+VERSION+")");
        if (version < VERSION) {
          log("Upgrading from store version "+version+" to "+VERSION);
          gcFrequency = 0x4000000000000000L; // disable GC while converting
          if (version < 25)
            chunkManager = new ChunkManager02(disk);
          else if (version < 290)
            chunkManager = new ChunkManager025(disk);
          else if (version < 293 && VERSION >= 293)
            chunkManager = new DefaultChunkManager(disk, version);
            
          // snapshot will notice if there are two chunk managers and re-store all chunks
          try {
            acquireWriteLock();
          } catch (InterruptedException e) {
            throw new InternalSmyleError(e.toString());
          }
          SnapshotImpl snapshot = new SnapshotImpl(this, writeChunkManager, true);
          ++snapshot.references;
          snapshot.commit();
	        //snapshot.save(); // why?
          chunkManager = writeChunkManager;
          gcFrequency = DEFAULT_GC_FREQUENCY; // re-enable GC
        }
        
        if (debug) System.out.println("Collecting garbage");
        collectGarbage(false);
      } else if (readOnly)
	throw new StoreNotFoundException("No store found in "+disk);
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  public synchronized void addReference() {
    ++references;
  }
  
  private void acquireWriteLock() throws InterruptedException {
    Thread thread = Thread.currentThread();
    if (writeLock != null || waiters != 0) {
      if (writingThread == thread) {
        if (logger != null)
          writeLock.printStackTrace(logger);
        throw new MultipleMutableSnapshotsException(
          "This thread already holds a mutable snapshot - please release before acquiring a new one");
      }
      ++waiters;
      try {
        if (timeout != 0) {
          //System.out.println("Thread "+thread+" waiting for "+timeout+" ms");
          wait(timeout);
        } else {
          /*System.out.println("Thread "+thread+" waiting");
          new Throwable().printStackTrace();*/
          wait();
        }
      } finally {
        --waiters;
      }
      if (writeLock != null) {
        if (logger != null)
          writeLock.printStackTrace(logger);
        throw new TimeoutException(thread+" failed to acquire write lock after waiting "+timeout+" ms");
      }
     }
    writeLock = new Throwable("Current write lock was acquired by "+thread+" here:");
    writingThread = thread;
  }
  
  private void flush(boolean laterToo) throws InterruptedException {
    SnapshotImpl s;
    synchronized(this) {
      if (flusher != null) {
        flusher.interrupt();
        flusher = null;
      }

      if (unwrittenSnapshot != null) {
        acquireWriteLock();
        s = unwrittenSnapshot;
        unwrittenSnapshot = null;
      } else {
        if (laterToo && writeLock != null)
          deferNextCommit = false;
        return;
      }
    }
    if (s != null)
      s.save();
    releaseWriteLock();
  }

  synchronized void releaseWriteLock() {
    cachedSnapshot = null;
    if (writeSnapshot != null) {
      if (writeSnapshot.unwritten) 
        unwrittenSnapshot = writeSnapshot;
      writeSnapshot = null; 
    }
    writeLock = null;
    writingThread = null;
    //System.out.println(Thread.currentThread()+" notify, writeLock="+writeLock);
    notify();
  }

  public synchronized Snapshot mutableSnapshot() {
    assertOpen();
    assertWritable();

    try {
      acquireWriteLock();
    } catch (InterruptedException e) {
      throw new InternalSmyleError(e.toString());
    }
    if (unwrittenSnapshot != null) {
      writeSnapshot = unwrittenSnapshot;
      unwrittenSnapshot = null;
      cachedSnapshot = null;
    } else
      writeSnapshot = getSnapshot(true);
    ++writeSnapshot.references;
    return writeSnapshot;
  }
  
  private SnapshotImpl getSnapshot(boolean mutable) {
    SnapshotImpl result = new SnapshotImpl(this, chunkManager, mutable);
    activeSnapshots.add(new WeakReference<SnapshotImpl>(result));
    return result;
  }
  
  public synchronized Snapshot snapshot() {
    assertOpen();
    return immutableSnapshot();
  }
  
  private SnapshotImpl immutableSnapshot() {
    try {
      flush(false);
    } catch (InterruptedException e) {
      throw new InternalSmyleError(e.toString());
    }
    if (cachedSnapshot == null) {
      cachedSnapshot = getSnapshot(false);
    }
    ++cachedSnapshot.references;
    return cachedSnapshot;
  }
  
  public synchronized void close() {
    assertOpen();
    if (references == 1) {
      synchronized(StoreRegistry.class) { // don't want to be reopened while gc runs
        forgetSnapshots();
        try {
          flush(false);
        } catch (InterruptedException e) {
          throw new InternalSmyleError(e.toString());
        }
        collectGarbage(false);

      	//log(getStats());

        StoreRegistry.removeStore(this);
        disk.release();
      }
    }
    --references;
  }
  
  public synchronized String getStats() {
    return writeChunkManager.getStats();
  }
  
  void assertOpen() throws ClosedStoreException {
    if (references <= 0)
      throw new ClosedStoreException("This store has been closed");
  }
  
  void assertWritable() throws ReadOnlyException {
    if (readOnly)
      throw new ReadOnlyException("Store was opened in read-only mode");
  }
  
  /*public void deleteEverything() {
    disk.deleteEverything();
  }*/
  
  public synchronized void collectGarbage() {
    collectGarbage(true);
  }
  
  public synchronized void collectGarbage(boolean memGC) {
    if (readOnly) return;
    Snapshot snapshot = mutableSnapshot();
    collectGarbageNoLock(memGC);
    snapshot.commit();
  }
  
  private void collectGarbageNoLock(boolean memGC) {
    if (readOnly) return;
    SnapshotImpl current;
    ArrayList<WeakReference<SnapshotImpl>> snapshots;
    synchronized(this) {
      current = immutableSnapshot();
      snapshots = new ArrayList(activeSnapshots);
    }
    
    if (memGC) {
      // search for any active snapshots that preserve an older state
      boolean needMemGC = false;
      for (int i = 0; i < snapshots.size(); i++) {
        SnapshotImpl snapshot = snapshots.get(i).get();
        if (snapshot != null && !snapshot.equals(current)
          && !(snapshot == cachedSnapshot && snapshot.references == 1)) {
          if (debug) System.out.println("Need mem GC, snapshot open: " + snapshot);
          needMemGC = true;
          break;
        }
      }
      
      // free all snapshots that were not forgotten or committed properly
      if (needMemGC)
        System.gc(); 
    }
    
    current.forget();
    
    BitSet whiteList = new BitSet();
    //if (debug) System.out.println("GC: whitelist length for current: "+whiteList.size());
    
    synchronized(this) {
      if (writingThread != Thread.currentThread())
        throw new InternalSmyleError("Must have write lock");
    }
      
    for (int i = 0; i < snapshots.size(); i++) {
      SnapshotImpl snapshot = snapshots.get(i).get();
      if (snapshot != null)
        snapshot.collectChunks(whiteList);
    }
      
    //if (debug) System.out.println("GC: disk master="+disk.getMasterFile()+", snapshot master="+current.master+", whitelist length="+whiteList.size());
    chunkManager.deleteEverythingBut(whiteList);
  }
  
  public synchronized ChunkManager getChunkManager() {
    return chunkManager;
  }
  
  public synchronized void setTimeout(int ms) {
    timeout = ms;
  }
    
  public synchronized void logTo(PrintWriter writer) {
    this.logger = writer;
  }
  
  public synchronized boolean exclusiveWriteLocking() {
    return exclusiveWriteLocking;
  }
  
  void forgetSnapshot(SnapshotImpl s) {
    for (int i = 0; i < activeSnapshots.size(); i++) {
      SnapshotImpl snapshot = activeSnapshots.get(i).get();
      
      // use occasion to compact the vector
      if (snapshot == null || snapshot == s)
        activeSnapshots.remove(i--);
    }
  }
  
  void forgetSnapshots() {
    activeSnapshots.clear();
    releaseWriteLock();
  }
  
  public synchronized void setGCFrequency(int bytes) {
    gcFrequency = bytes;
  }
  
  public synchronized void setClusterSize(int bytes) {
    disk.setClusterSize(bytes);
  }
  
  void maybeGC() {
    //System.out.println("maybeGC: written="+disk.totalBytesWritten()+", last="+lastGC+", freq="+gcFrequency);
    boolean needGC = false;
    synchronized(this) {
      if (disk.totalBytesWritten() >= lastGC+gcFrequency) {
        lastGC = disk.totalBytesWritten();
        needGC = true;
      }
    }
    
    if (needGC) {
      long startTime = System.currentTimeMillis();
      collectGarbageNoLock(true);
      long endTime = System.currentTimeMillis();
      
      if (showGC) log("GC ("+(endTime-startTime)+" ms)");
      
      synchronized(this) {
        lastGC = Math.max(lastGC, disk.totalBytesWritten());
      }
    }
  }
  
  public synchronized void enableIndexing() {
    if (indexAdvisors == null) {
      try { 
        flush(false);
      } catch (InterruptedException e) {
        throw new InternalSmyleError(e.toString());
      }
      indexAdvisors = new HashMap<String,IndexAdvisor<Pair<IndexProfile,Function>>>();
    }
  }
  
  synchronized IndexAdvisor<Pair<IndexProfile,Function>> getIndexAdvisor(String table,
    StructInfo structInfo) {
    if (indexAdvisors == null)
      return null; // indexing disabled
    IndexAdvisor<Pair<IndexProfile,Function>> ia = indexAdvisors.get(table);
    if (ia == null)
      indexAdvisors.put(table, ia = new IndexAdvisor<Pair<IndexProfile,Function>>(/*
        new PairMarDemar<IndexProfile,Function>(new FunctionMarDemar(structInfo))*/));
    return ia;
  }
  
  synchronized ChunkRef saveMaster(Buffer buffer) {
    masterWriteSite = new Throwable("Writing master");
    cachedSnapshot = null;
    return writeChunkManager.createMasterChunk(buffer);
  }

  public synchronized void setWriteLatency(int ms) {
    writeLatency = ms;
    deferNextCommit = ms != 0;
  }

  synchronized void scheduleFlush() {
    if (flusher == null) {
      flusher = new Thread() {
        public void run() {
          try { 
            Thread.sleep(writeLatency);
            synchronized(DiskStore.this) {
              if (references <= 0) return;
            }
            //System.out.println("Flusher "+this+" flushing");
            flush(true);
          } catch (InterruptedException e) {
          }
        }
      };
      flusher.start();
    }
  }

  public synchronized void kill() {
    if (flusher != null) {
      flusher.interrupt();
      flusher = null;
    }
  }

  synchronized boolean deferThisCommit() {
    boolean result = deferNextCommit;
    deferNextCommit = writeLatency != 0;
    return result;
  }

  void log(String msg) {
    if (logger != null)
      logger.println("Smyle: "+msg);
  }
  
  public synchronized void optimize() {
    writeChunkManager.reorderAllChunks();
  }
  
  public synchronized ArrayList<SnapshotImpl> activeSnapshots() {
    ArrayList<SnapshotImpl> result = new ArrayList<SnapshotImpl>();
    for (int i = 0; i < activeSnapshots.size(); i++) {
      SnapshotImpl snapshot = activeSnapshots.get(i).get();
      if (snapshot != null)
        result.add(snapshot);
    }
    return result;
  }
  
  public synchronized void clearCaches() {
    if (cachedSnapshot != null) {
      cachedSnapshot.forget();
      cachedSnapshot = null;
    }
    writeChunkManager.clearCaches();
  }
}
