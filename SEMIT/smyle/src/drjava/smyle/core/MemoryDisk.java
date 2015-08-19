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
import junit.framework.*;
import org.artsProject.mcop.*;
import drjava.smyle.*;

public class MemoryDisk implements Disk {
  long idCounter = 0, master = 0, bytesWritten = 0;
  int clusterSize = 16;
  HashMap<Long,FileImpl> files = new HashMap<Long,FileImpl>();
  public boolean sabotageMasterWrite = false, readOnly = false;
  
  class FileImpl implements Disk.NewFile {
    long id = ++idCounter;
    byte[] data = null;
    boolean isMaster, sabotage;
    
    OutputStream out = new ByteArrayOutputStream() {
      public void close() throws IOException {
        super.close();
        if (sabotage) {
          data = new byte[0];
          throw new SmyleIOException("sabotage");
        } else {
          data = toByteArray();
          bytesWritten += (data.length+clusterSize-1)/clusterSize*clusterSize;
          if (isMaster)
            master = id;
        }
      }
    };
    
    public long getId() {
      return id;
    }
    
    public OutputStream getOutputStream() {
      if (out == null)
        Assert.fail("getOutputStream may only be called once");
      OutputStream result = out;
      out = null;
      return result;
    }
  }
    
  FileImpl _createFile() {
    FileImpl file = new FileImpl();
    files.put(new Long(file.id), file);
    return file;
  }
  
  public Disk.NewFile createFile() {
    assertWritable();
    return _createFile();
  }
  
  public long saveMaster(Buffer data) {
    assertWritable();
    FileImpl file = _createFile();
    file.isMaster = true;
    file.sabotage = sabotageMasterWrite;
    DiskUtil.bufferToFile(file, data);
    return file.getId();
  }
  
  public long getMasterFile() {
    return master;
  }
  
  public int getFileLength(long id) {
    try {
      FileImpl file = getFile(id);
      return file.data.length;
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  public InputStream readFile(long id) throws IOException {
    FileImpl file = getFile(id);
    return new ByteArrayInputStream(file.data);
  }
  
  FileImpl getFile(long id) throws IOException {
    FileImpl file = files.get(new Long(id));
    if (file == null)
      throw new FileNotFoundException(String.valueOf(id));
    if (file.data == null)
      throw new SmyleIOException("File was not closed or is still being written");
    return file;
  }
    
  public void deleteEverything() {
    assertWritable();
    files.clear();
    idCounter = master = 0;
  }
  
  public int numberOfFiles() {
    return files.size();
  }
  
  public void deleteEverythingBut(Set<FileRef> whiteList) {
    assertWritable();
    for (Iterator<Long> i = files.keySet().iterator(); i.hasNext(); ) {
      long nr = i.next().longValue();
      if (!whiteList.contains(new FileRef(nr))) {
        //System.out.println("MemoryDisk: removing file "+nr+" (whitelist length: "+whiteList.size()+")");
        /*for (Iterator<FileRef> it = whiteList.iterator(); it.hasNext(); )
          System.out.println("Whitelist entry: "+it.next().id);*/
        i.remove();
      }
    }
  }
  
  public long totalBytes() {
    long total = 0;
    for (Iterator<FileImpl> i = files.values().iterator(); i.hasNext(); ) {
      byte[] data = i.next().data;
      if (data != null)
        total += data.length;
    }
    return total;
  }
  
  /** this is just for generating test cases, so 32-bit ids are ok */
  public void marshal(Buffer buffer) {
    buffer.writeLong(files.size());
    for (Iterator<FileImpl> i = files.values().iterator(); i.hasNext(); ) {
      FileImpl f = i.next();
      buffer.writeLong((int) f.id);
      buffer.writeLong(f.data.length);
      buffer.writeBytes(f.data);
    }
    buffer.writeLong((int) master);
    buffer.writeLong((int) idCounter);
  }
  
  public MemoryDisk(Buffer buffer) {
    int n = buffer.readLong();
    for (int i = 0; i < n; i++) {
      FileImpl f = new FileImpl();
      f.id = buffer.readLong();
      int len = buffer.readLong();
      f.data = buffer.readBytes(len);
      files.put(new Long(f.id), f);
    }
    master = buffer.readLong();
    idCounter = buffer.readLong();
  }
  
  public MemoryDisk() {
  }
  
  public synchronized long totalBytesWritten() {
    return bytesWritten;
  }
  
  public synchronized void setClusterSize(int bytes) {
    clusterSize = bytes;
  }
  
  public synchronized void release() {
  }

  private void assertWritable() {
    if (readOnly)
      throw new RuntimeException("Read only disk");
  }
}

