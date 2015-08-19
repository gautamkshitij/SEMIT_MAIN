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
import org.artsProject.mcop.*;
import drjava.smyle.*;
import drjava.smyle.core.*;

// TODO: add another abstraction layer between this and raw disk storage
//       (without master handling) to be able to test this class against
//       disk failures

public class FileSystemDisk implements Disk {
  long idCounter, master, bytesWritten = 0;
  File dir, inUse;
  //String prefix = createPrefix();
  int diskId, diskIdRange;
  int clusterSize = DEFAULT_CLUSTERSIZE;
  ArrayList<NormalFileOutputStream> openStreams
    = new ArrayList<NormalFileOutputStream>();
  
  public static final String
    EXT = ".smy",
    MASTER = "master";
    
  static final int
    DEFAULT_CLUSTERSIZE = 32768;
    
  /*static String createPrefix() {
    return new String(System.currentTimeMillis()/100)
      +new String((random.nextInt() & 0x7fffffff) % 1000)
      +"_";
  }*/
    
  class NormalFileOutputStream extends FileOutputStream {
    int length = 0;
    
    NormalFileOutputStream(File file) throws IOException {
      super(file);
      openStreams.add(this);
    }
    
    public void write(int b) throws IOException {
      super.write(b);
      ++length;
    }
    
    public void write(byte[] b) throws IOException {
      super.write(b);
      length += b.length;
    }
    
    public void write(byte[] b, int ofs, int len) throws IOException {
      super.write(b, ofs, len);
      length += len;
    }
    
    public void close() throws IOException {
      super.close();
      synchronized (FileSystemDisk.this) { openStreams.remove(this); }
      bytesWritten += (length+clusterSize-1)/clusterSize*clusterSize;
    }
  }
  
  class FileImpl implements Disk.NewFile {
    long id = (++idCounter)*diskIdRange+diskId;
    OutputStream out;
    
    FileImpl() {
      try {
        out = new NormalFileOutputStream(getFile(id));
      } catch (IOException e) {
        throw new SmyleIOException(e);
      }
    }
    
    public long getId() {
      return id;
    }
    
    public synchronized OutputStream getOutputStream() {
      if (out == null)
        throw new BadUseException("getOutputStream may only be called once");
      OutputStream result = out;
      out = null;
      return result;
    }
  }
  
  public FileSystemDisk(File dir, boolean readOnly) {
    this(dir, 0, 1, readOnly);
  }
  
  public FileSystemDisk(File dir, int diskId, int diskIdRange, boolean readOnly) {
    try {
      this.diskId = diskId;
      this.diskIdRange = diskIdRange;
      this.dir = dir;
      if (!this.dir.isDirectory())
	if (readOnly)
	  throw new StoreNotFoundException(dir+" doesn't exist");
	else if (!this.dir.mkdir())
          throw new SmyleIOException("Can't create "+this.dir);
        
      // check and set in-use flag
      if (!readOnly) {
        File inUse = new File(dir, "in-use.flag");
        if (!inUse.createNewFile()) {
          throw new DiskInUseException("The directory\n  "
            +this.dir.getAbsolutePath()
            +"\nis already in use by another process or an instance of "
            +"Smyle loaded by a different class loader.\nIf you think this "
            +"is not the case, please delete the file\n  "+inUse.getAbsolutePath());
        }
        this.inUse = inUse;
        inUse.deleteOnExit();
      }
        
      // find number to count on from and new-style master file
      scanIds();
      
      // load master file if existent
      File f = new File(dir, MASTER+EXT);
      if (f.exists()) {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        int l = (int) f.length();
        if (l != 4 && l != 8)
          throw new SmyleIOException(f+": wrong file size ("+l+")");
        byte[] data = new byte[l];
        in.readFully(data);
        in.close();
        if (l == 4)
          master = new Buffer(data).readLong();
        else
          master = new Buffer(data).readLongLong();
      }
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  synchronized File getFile(long id) {
    return new File(dir, String.valueOf(id)+EXT);
  }
  
  synchronized File getMasterFile(long id) {
    return new File(dir, "m"+id+EXT);
  }
  
  /** finds the file with the given id whether it's a master file
      or not */
  private File smartGetFile(long id) {
    File f = getFile(id);
    return f.exists() ? f : getMasterFile(id);
  }
  
  public synchronized Disk.NewFile createFile() {
    return new FileImpl();
  }
  
  public synchronized long saveMaster(Buffer data) {
    FileImpl file = new FileImpl();
    DiskUtil.bufferToFile(file, data);
    setMaster(file.getId());
    return file.getId();
  }
  
  synchronized void setMaster(long id) {
    File f1 = getFile(id);
    File f2 = getMasterFile(id);
    if (!f1.renameTo(f2))
      throw new SmyleIOException("Couldn't rename "+f1+" to "+f2
        +"("+f1.exists()+"/"+f2.exists()+")");
    master = id;
  }
  
  public synchronized long getMasterFile() {
    return master;
  }
  
  public synchronized int getFileLength(long id) {
    try {
      File file = smartGetFile(id);
      int result = (int) file.length();
      if (result == 0 && !file.isFile())
        throw new FileNotFoundException(file.getPath());
      return result;
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  public synchronized InputStream readFile(long id) throws IOException {
    return new FileInputStream(smartGetFile(id));
  }
  
  private void closeOpenStreams() {
    for (int i = 0; i < openStreams.size(); i++) try {
      openStreams.get(i).close();
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
    openStreams.clear();
  }
  
  public synchronized void deleteEverything() {
    closeOpenStreams();
    
    deleteEverythingBut(new HashSet<FileRef>());
    File masterFile = new File(dir, MASTER+EXT);
    if (masterFile.isFile())
      if (!masterFile.delete())
        throw new SmyleIOException("Couldn't delete "+master);
    idCounter = master = 0;
  }
  
  public synchronized void deleteEverythingBut(Set<FileRef> whitelist) {
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      String name = files[i].getName().toLowerCase();
      if (files[i].isFile() && name.endsWith(EXT)) try {
        long id = Long.parseLong(name.substring(
          name.startsWith("m") ? 1 : 0, name.indexOf('.')));
        if (!whitelist.contains(new FileRef(id))) {
          if (!files[i].delete())
            throw new SmyleIOException("Couldn't delete "+files[i]);
        }
      } catch (NumberFormatException e) {
        e.printStackTrace();
        // Couldn't parse number, ignore
      }
    }
  }
  
  synchronized void scanIds() {
    idCounter = master = 0;
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      String name = files[i].getName().toLowerCase();
      if (files[i].isFile() && name.endsWith(EXT)) try {
        long id;
        if (name.startsWith("m")) {
          // master file
          id = Long.parseLong(name.substring(1, name.indexOf('.')));
          if (id > master) master = id;
        } else {
          // regular file
          id = Long.parseLong(name.substring(0, name.indexOf('.')));
        }
        if (id > idCounter) idCounter = id;
      } catch (Exception e) {
        // Couldn't parse number, ignore
      }
    }
  }
  
  public synchronized void setIdCounter(long counter) {
    this.idCounter = counter;
  }
  
  public synchronized long totalBytesWritten() {
    return bytesWritten;
  }
  
  public synchronized void setClusterSize(int bytes) {
    clusterSize = bytes;
  }
  
  public synchronized void release() {
    closeOpenStreams();
    if (inUse != null) {
      inUse.delete();
      inUse = null;
    }
  }
  
  public synchronized void finalize() {
    if (inUse != null)
      release();
  }

  public synchronized String toString() {
    return dir.toString();
  }

  public synchronized boolean inUse() {
    return new File(dir, "in-use.flag").exists();
  }
}

