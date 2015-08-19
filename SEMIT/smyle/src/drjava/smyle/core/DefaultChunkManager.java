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
import drjava.util.*;
import drjava.gjutil.*;
import drjava.smyle.*;

public class DefaultChunkManager extends MasterChunkManager {
  static final int
    DEFAULT_NATURALFILESIZE = 64*1024,
    MAXCACHESIZE = DEFAULT_NATURALFILESIZE;
    
  public static boolean 
    debug = false,
    verbose = false,
    COMPRESSION = false,
    fullStats = false,
    localityOptimization = true;
    
  Disk disk;
  int version;
  ArrayList<Chunk> chunks0 = new ArrayList<Chunk>();
  ArrayList<Chunk> newChunks0 = chunks0;
  Disk.NewFile newFile[] = new Disk.NewFile[2];
  Buffer newFileBuf[] = new Buffer[2];
  boolean dirty = false;
  ChunkRef masterChunk1 = NULLCHUNK;
  int naturalFileSize = DEFAULT_NATURALFILESIZE;
  boolean masterLoaded = false;
  int firstEmptyChunk0 = 0;
  Cache<Long,byte[]> fileCache = new Cache<Long,byte[]>(1);
  
  /** only needed during gc */
  Set<FileRef> fileWhiteList = null;
  
  static final Chunk emptyChunk = new Chunk();
  static final MarDemar<Chunk> chunkMarDemar =
    new TypeMarDemar<Chunk>(Chunk.DEMARSHALLER);
  
  ChunkManager cm0 = new ChunkManager() {
    public Buffer readChunk(int chunk) {
      return decompress(readChunk0(chunk));
    }
    
    public ChunkRef createChunk(Buffer data) {
      return createChunk0(compress(data));
    }
  };
  
  ScalablePool<Chunk> chunks1 = new ScalablePool(chunkMarDemar, cm0, null);
  ChunkRef chunks1Ref;

  // stats
  long[] lastAccessedFile = new long[2];
  int[] chunksRead = new int[2], fileHops = new int[2];
  Integer lastAccessedChunk = new Integer(0);
  HashMap<Integer,Integer> accessSequence = new HashMap<Integer,Integer>();
  
  // full stats
  TreeMap<Pair<Integer,Integer>,Integer> accessPairs;
  
  public DefaultChunkManager(Disk disk, int version) {
    this.disk = disk;
    this.version = version;
  }
  
  public synchronized void setNaturalFileSize(int naturalFileSize) {
    this.naturalFileSize = naturalFileSize;
  }
  
  // accessors
  
  public synchronized Buffer readChunk(int index) {
    return decompress(readChunk1(index));
  }
  
  private Buffer readChunk1(int index) {
    if (index <= 0)
      throw new IllegalArgumentException("Invalid ChunkRef (index="+index+")");
    Chunk chunk = chunks1.get(index-1);
    if (chunk == null)
      throw new InternalSmyleError("Chunk "+index+" doesn't exist anymore");
      
    if (localityOptimization && index != lastAccessedChunk.intValue()) {
      Integer c = new Integer(index);
      accessSequence.put(lastAccessedChunk, c);
      
      if (fullStats) {
        if (accessPairs == null)
          accessPairs = new TreeMap<Pair<Integer,Integer>,Integer>();
        Pair<Integer,Integer> p = new Pair(lastAccessedChunk, c);
        Integer I = accessPairs.get(p);
        accessPairs.put(p, new Integer(I == null ? 1 : I.intValue()+1));
      }
      
      lastAccessedChunk = c;
    }
    
    return loadChunk(chunk, 1);
  }
  
  private Buffer readChunk0(int index) {
    return loadChunk(chunks0.get(index-1), 0);
  }
  
  private Buffer loadChunk(Chunk chunk, int i) {
    chunksRead[i]++;
    if (chunk.file != lastAccessedFile[i]) {
      lastAccessedFile[i] = chunk.file;
      fileHops[i]++;
    }

    //System.out.println(chunk+", newFile="+(newFile != null ? newFile.getId() : 0));
    try {
      if (newFile[i] != null && chunk.file == newFile[i].getId())
        // chunk is part of newFile
        return new Buffer(newFileBuf[i], chunk.offset, chunk.length);
      
      if (chunk.file == 0) {
        throw new InternalSmyleError("Chunk doesn't exist anymore");
      } else {
        // look in cache
        Long lFile = new Long(chunk.file);
        byte[] data = fileCache.get(lFile);
        if (data != null)
          return new Buffer(data, chunk.offset, chunk.length);
        
        // read chunk from file
        DataInputStream in = new DataInputStream(disk.readFile(chunk.file));
        //System.out.println("Reading file "+chunk.file);
        
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
          return new Buffer(data, 0, chunk.length);
        }
      }
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  /** only needed for CompressionBench */
  public synchronized Iterator<ChunkRef> chunks1() {
    ArrayList<ChunkRef> l = new ArrayList<ChunkRef>();
    for (int i = 1; i <= chunks1.size(); i++) {
      if (chunks1.get(i-1) != null)
        l.add(new ChunkRef(i));
    }
    return l.iterator();
  }
  
  public synchronized ChunkRef getMasterChunk() {
    if (!masterLoaded) {
      masterLoaded = true;
      loadMaster();
    }
    return masterChunk1;
  }
  
  // mutators
  
  private ChunkRef createChunk0(Buffer data) {
    // reuse empty chunk index if possible
    int index;
    for (index = firstEmptyChunk0; index < newChunks0.size(); index++) {
      if (newChunks0.get(index).file == 0) {
        //System.out.println("Reusing chunk "+index);
        break;
      }
    }
    
    firstEmptyChunk0 = index+1;
    saveChunk0(index+1, data);
    return new ChunkRef(index+1);
  }
  
  public synchronized ChunkRef createChunk(Buffer data) {
    int index = chunks1.insert(new Chunk());
    chunks1.replace(index, saveChunk(compress(data), 1));
    return new ChunkRef(index+1);
  }
  
  void saveChunk0(int index, Buffer data) {
    setOrAdd(newChunks0, index-1, saveChunk(data, 0));
  }
  
  void saveChunk1(int index, Buffer data) {
    chunks1.replace(index-1, saveChunk(data, 1));
  }
  
  Chunk saveChunk(Buffer data, int i) {
    dirty = true;
    if (data.remaining() > naturalFileSize) {
      // too large, needs a file of its own
      
      int len = data.remaining();
      return new Chunk(DiskUtil.bufferToFile(disk, data).id, 0, len);
    } else {
      // put in combined file
      
      if (newFile[i] != null && newFileBuf[i].remaining()+data.remaining() > naturalFileSize) {
        // need to flush newFile
        
        /*if (newFile != null)
          System.out.println("Autosaving new file ("+newFile.remaining()+" bytes)");*/
        saveNewFile(i);
      }
      
      if (newFile[i] == null) {
        newFile[i] = disk.createFile();
        newFileBuf[i] = new Buffer();
      }
      
      Chunk chunk = new Chunk(newFile[i].getId(),
        newFileBuf[i].remaining(), data.remaining());
      newFileBuf[i].writeBuffer(data);
      return chunk;
    }
  }
  
  static void setOrAdd(ArrayList<Chunk> v, int index, Chunk c) {
    while (index > v.size())
      v.add(emptyChunk);
    if (index < v.size())
      v.set(index, c);
    else
      v.add(c);
  }
  
  Buffer compress(Buffer b) {
    if (COMPRESSION) {
      Buffer b2 = new Buffer();
      Compression.compress(new Buffer(b), b2);
      //System.out.println("Compressed   "+b.remaining()+" "+b2.remaining());
      return b2;
    }
    return b;
  }
  
  Buffer decompress(Buffer b) {
    if (version >= 293 && COMPRESSION) {
      Buffer b2 = new Buffer();
      Compression.decompress(new Buffer(b), b2);
      //System.out.println("Decompressed "+b2.remaining()+" "+b.remaining());
      return b2;
    }
    return b;
  }
  
  void saveMaster() {
    try {
      Buffer b = new Buffer();
      b.willWrite(chunks0.size()*16+4+4);
      /*System.out.println("Writing "+chunks0.size()+"-"+emptyChunks(chunks0)
        +" chunks to master");*/
      writeChunkSeq(b, chunks0);
      chunks1Ref.marshal(b);
      masterChunk1.writeType(b);
      
      if (debug) {
        System.out.println("Size of master buffer: "+b.remaining()+" ("
          +emptyChunks1()+" empty chunks out of "+chunks1.size()+")");
      }
      
      Buffer b2 = new Buffer();
      b2.writeLong(version);
      //Buffer bb = new Buffer(b);
      if (COMPRESSION)
        Compression.compress(b, b2);
      else
        b2.writeBuffer(b);
      //System.out.println(bb+" -> "+b2);
      disk.saveMaster(b2);
    } catch (SmyleIOException e) {
      // If master write failed, reload master to cleanly undo changes
      loadMaster();
      throw e;
    }
  }
  
  void loadMaster() {
    long master = disk.getMasterFile();
    if (master != 0) {
      Buffer b = DiskUtil.fileToBuffer(disk, master);
      b.readLong(); // version
      //Buffer b2 = b;
      b = decompress(b);
      //System.out.println(b2+" -> "+b);
      chunks0.clear();
      firstEmptyChunk0 = 0;
      readChunkSeq(b, chunks0);
      chunks1 = new ScalablePool(chunkMarDemar, cm0, new ChunkRef(b));
      masterChunk1 = new ChunkRef(b);
      accessSequence.clear();
    }
  }
  
  private void writeChunkSeq(Buffer b, List<Chunk> chunks) {
    b.writeLong(chunks.size());
    for (int i = 0; i < chunks.size(); i += 32) {
      int val = 0;
      for (int j = 0; j < 32 && i+j < chunks.size(); j++)
        if (!chunks.get(i+j).equals(emptyChunk))
          val |= 1 << j;
      b.writeLong(val);
      for (int j = 0; j < 32 && i+j < chunks.size(); j++)
        if (!chunks.get(i+j).equals(emptyChunk))
          chunks.get(i+j).marshal(b);
    }
  }
  
  private void readChunkSeq(Buffer b, List<Chunk> chunks) {
    int n = b.readLong();
    for (int i = 0; i < n; i += 32) {
      int val = b.readLong();
      for (int j = 0; j < 32 && i+j < n; j++)
        if ((val & (1 << j)) != 0)
          chunks.add(new Chunk(b));
        else
          chunks.add(emptyChunk);
    }
  }
  
  public synchronized ChunkRef createMasterChunk(Buffer data) {
    masterChunk1 = createChunk(data);
    flush();
    return masterChunk1;
  }
  
  void saveNewFile(int i) {
    if (newFile[i] != null) {
      DiskUtil.bufferToFile(newFile[i], newFileBuf[i]);
      if (fileWhiteList != null)
        fileWhiteList.add(new FileRef(newFile[i].getId()));
        
      newFile[i] = null;
      newFileBuf[i] = null;
    }
  }
  
  public synchronized void flush() {
    if (dirty) {
      saveChunks1();
      saveNewFile(0);
      saveNewFile(1);
      saveMaster();
      dirty = false;
    }
  }
  
  private boolean chunkInSmallFile(HashSet<FileRef> fileWhiteList,
    HashSet<FileRef> sizeChecked, Chunk chunk) {
    if (chunk.file > 0) {
      FileRef fr = new FileRef(chunk.file);
      if (fileWhiteList.contains(fr) && !sizeChecked.contains(fr)) {
        sizeChecked.add(fr);
        if ((newFile[0] == null || fr.id != newFile[0].getId())
          && (newFile[1] == null || fr.id != newFile[1].getId())
          && disk.getFileLength(fr.id) <= naturalFileSize/2)
          return true;
      }
    }
    return false;
  }
  
  private void genNewChunks0() {
    newChunks0 = new ArrayList<Chunk>();
    firstEmptyChunk0 = 0;
    BitSet chunks0WhiteList = new BitSet();
    chunks1.collectChunks(chunks0WhiteList);
    int len0 = chunks0.size()+1, retained0 = 0, dropped0 = 0;
    for (int index = 1; index < len0; index++) {
      if (chunks0WhiteList.get(index)) {
        Chunk chunk = chunks0.get(index-1);
        fileWhiteList.add(new FileRef(chunk.file));
        setOrAdd(newChunks0, index-1, chunk);
        ++retained0;
      } else if (chunks0.get(index-1).file != 0)
        ++dropped0;
    }
    /*System.out.println("0-chunks retained: "+retained0+"/"+(len0-1)
      +", dropped: "+dropped0+", empty: "
      +emptyChunks(chunks0)+"->"+emptyChunks(newChunks0));*/
  }

  public synchronized void deleteEverythingBut(final BitSet chunks1WhiteList) {
   new Benchmark() { protected void action() {
    HashSet<FileRef> fileWhiteList = new HashSet<FileRef>();
    // make sure newly created files are added to list
    DefaultChunkManager.this.fileWhiteList = fileWhiteList;
    
    HashSet<FileRef> relocationList = new HashSet<FileRef>();
    HashSet<FileRef> sizeChecked = new HashSet<FileRef>();
    
    // release obsolete 1-chunks, add application chunks to fileWhiteList
    
    int len1 = chunks1.size()+1;
    //System.out.println("whiteList: "+whiteList+", l="+len);
    for (int index = 1; index < len1; index++) {
      boolean relocate = false;
      Chunk chunk = chunks1.get(index-1);
      if (chunk != null) {
        fileWhiteList.add(new FileRef(chunk.file));
        if (chunks1WhiteList.get(index)) {
          if (chunkInSmallFile(fileWhiteList, sizeChecked, chunk))
            relocate = true;
        } else {
          chunks1.release(index-1);
          relocate = true;
        }
      
        if (relocate) {
          FileRef fr = new FileRef(chunk.file);
          if (fileWhiteList.contains(fr)) {
            relocationList.add(fr);
            fileWhiteList.remove(fr);
          }
        }
      }
    }
    done("chunks1");
    dirty = true; flush();
    done("flush");
    
    // generate newChunks0 and add to fileWhiteList
    
    genNewChunks0();
    done("genNewChunks0");
    
    // continue filling relocationList
    // (files with obsolete chunks and small files)
    
    // 0-chunks
    for (int i = 0; i < chunks0.size(); i++) {
      Chunk chunk = chunks0.get(i);
      if (i >= newChunks0.size() || newChunks0.get(i).file == 0 ||
        chunkInSmallFile(fileWhiteList, sizeChecked, chunk)) {
        FileRef fr = new FileRef(chunk.file);
        if (fileWhiteList.contains(fr)) {
          relocationList.add(fr);
          fileWhiteList.remove(fr);
        }
      }
    }
    done("fill relocation list");

    // relocate chunks0 and chunks1
    
    if (!relocationList.isEmpty()) {
      
      for (int i = 0; i < newChunks0.size(); i++) {
        Chunk chunk = newChunks0.get(i);
        if (chunk.file != 0 && relocationList.contains(new FileRef(chunk.file))) {
          //System.out.println("Relocating chunk "+i+" of file "+chunk.file);
          saveChunk0(i+1, readChunk0(i+1));
        }
      }
      
      chunks0 = newChunks0;
      done("relocate chunks0");
    
      relocateChunks1(relocationList);
      if (verbose) System.out.println("Relocated "+relocationList.size()+" files");
      
      done("relocate chunks1");
      saveChunks1();
      done("saveChunks1");
      genNewChunks0();
      done("genNewChunks0");
    }
    
    chunks0 = newChunks0;
    
    // save master and add it to file list

    flushAndDeleteFiles();
    done("disk.deleteEverythingBut");
   }}.run/*AndPrint*/();
  }
  
  private void flushAndDeleteFiles() {
    dirty = true;
    flush();
    fileWhiteList.add(new FileRef(disk.getMasterFile()));

    accessSequence.clear();
      
    // remove garbage files
    
    if (verbose) System.out.println("Retaining "+fileWhiteList.size()+" files");
    disk.deleteEverythingBut(fileWhiteList);
    fileWhiteList = null;
  }
  
  private void relocateChunks1(HashSet<FileRef> relocationList) {
    for (int i = 1; i <= chunks1.size(); i++) {
      int j = i;
      Chunk chunk;
      while ((chunk = chunks1.get(j-1)) != null && relocationList.contains(new FileRef(chunk.file))) {
        //System.out.println("Relocating chunk "+j+" of file "+chunk.file);
        saveChunk1(j, readChunk1(j));
        if (localityOptimization) {
          Integer J = accessSequence.get(new Integer(j));
          if (J != null) j = J.intValue(); else break;
        } else break;
      }
    }
  }
  
  private void relocateChunks1() {
    BitSet relocated = new BitSet();
    for (int i = 1; i <= chunks1.size(); i++) {
      int j = i;
      Chunk chunk;
      while ((chunk = chunks1.get(j-1)) != null && !relocated.get(j)) {
        //System.out.println("Relocating chunk "+j+" of file "+chunk.file);
        saveChunk1(j, readChunk1(j));
        relocated.set(j);
        Integer J = accessSequence.get(new Integer(j));
        if (J != null) j = J.intValue(); else break;
      }
    }
  }
  
  public synchronized void reorderAllChunks() {
    fileWhiteList = new HashSet<FileRef>();
    relocateChunks1();
    saveChunks1();
    genNewChunks0();
    chunks0 = newChunks0;
    flushAndDeleteFiles();
  }
  
  public synchronized int numChunks() {
    return chunks1.size()-emptyChunks1();
  }
  
  private int emptyChunks(List<Chunk> l) {
    int n = 0;
    for (int i = 0; i < l.size(); i++)
      if (l.get(i).equals(emptyChunk)) n++;
    return n;
  }
  
  private int emptyChunks1() {
    int emptyChunks = 0;
    for (int i = 0; i < chunks1.size(); i++)
      if (chunks1.get(i) == null)
        ++emptyChunks;
    return emptyChunks;
  }
  
  private void saveChunks1() {
    chunks1Ref = chunks1.flush();
  }

  public synchronized String getStats() {
    long hard = 0, soft = 0;
    hard += chunks0.size()*4;
    for (int i = 0; i < chunks0.size(); i++)
      if (chunks0.get(i) != emptyChunk)
        hard += 16;
    if (newFileBuf[0] != null) hard += newFileBuf[0].remaining();
    if (newFileBuf[1] != null) hard += newFileBuf[1].remaining();
    
    {
      Iterator<byte[]> i = fileCache.values().iterator();
      if (i.hasNext()) {
        hard += i.next().length; // one of the values is hard referenced - just pretend it's this one
        while (i.hasNext())
          soft += i.next().length;
      }
    }
    
    String result =
      "Hard cached data: "+hard+", soft cached data: "+soft
      +"\n0-chunks read: "+chunksRead[0]+", file hops: "+fileHops[0]+(chunksRead[0] != 0 ? " ("+(fileHops[0]*100L/chunksRead[0])+"%)" : "")
      +"\n1-chunks read: "+chunksRead[1]+", file hops: "+fileHops[1]+(chunksRead[1] != 0 ? " ("+(fileHops[1]*100L/chunksRead[1])+"%)" : "")
      +", access sequence pairs: "+accessSequence.size();
      
    if (fullStats && accessPairs != null) {
      StringBuffer buf = new StringBuffer();
      for (Iterator<Map.Entry<Pair<Integer,Integer>,Integer>> i = accessPairs.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry<Pair<Integer,Integer>,Integer> e = i.next();
        buf.append('\n')
          .append(e.getKey().a()).append('>').append(e.getKey().b()).append(": ").append(e.getValue());
      }
      result += buf;
    }
    
    return result;
  }
  
  public synchronized void clearCaches() {
    fileCache = new Cache<Long,byte[]>(1);
    // todo: flush chunks0, chunks1?
  }    
}
