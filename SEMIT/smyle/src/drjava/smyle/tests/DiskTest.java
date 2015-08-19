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

package drjava.smyle.tests;

import java.io.*;
import junit.framework.*;
import java.util.*;
import drjava.util.*;
import drjava.gjutil.*;
import org.artsProject.mcop.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;
import drjava.smyle.core.*;

public class DiskTest extends TestCase {
  final Factory<Disk> diskFactory;
  Disk disk;
  
  public DiskTest(String name, Factory<Disk> diskFactory) {
    super(name);
    this.diskFactory = diskFactory;
  }
  
  public static Test suite(final Factory<Disk> diskFactory) {
    return new FactoryTestSuite(DiskTest.class, new TestFactory() {
      public TestCase createTest(String name) {
        return new DiskTest(name, diskFactory);
      }
    });
  }
  
  public static TestSuite suite() {
    return new AggregatedTestSuite("Disk Tests",
      new Test[] {
        // memory
        
        suite(new Factory<Disk>() {
          public Disk produce() {
            return new MemoryDisk();
          }
        }),
        
        // file system
        
        suite(new Factory<Disk>() {
          public Disk produce() {
            return new FileSystemDisk(new File("temp/disktest"), false);
          }
        })
      }
    );
  }
  
  public void setUp() throws Exception {
    createDisk(true);
  }
  
  public void tearDown() throws Exception {
    if (disk != null)
      disk.release();
  }
  
  void createDisk(boolean wipe) throws Exception {
    disk = diskFactory.produce();
    if (wipe) disk.deleteEverything();
  }
  
  void assertFileDoesntExist(long id) throws Exception {
    try {
      disk.readFile(id).close();
      fail("File "+id+" should not exist");
    } catch (FileNotFoundException e) {
      // ok
    }
  }
  
  public void testDeleteEverythingBut() throws Exception {
    Disk.NewFile file1 = disk.createFile(); file1.getOutputStream().close();
    Disk.NewFile file2 = disk.createFile(); file2.getOutputStream().close();
    Disk.NewFile file3 = disk.createFile(); file3.getOutputStream().close();
    HashSet<FileRef> whitelist = new HashSet<FileRef>();
    whitelist.add(new FileRef(file2.getId()));
    
    disk.deleteEverythingBut(whitelist);
    
    // assert file2 exists
    disk.readFile(file2.getId()).close();
    
    // assert file1 and file3 have been deleted
    assertFileDoesntExist(file1.getId());
    assertFileDoesntExist(file3.getId());
  }
  
  public void testTotalBytesWritten() throws Exception {
    disk.setClusterSize(6);
    
    Buffer b = new Buffer();
    b.writeLong(123);
    DiskUtil.bufferToFile(disk, b);
    assertEquals(6, disk.totalBytesWritten());
    
    b = new Buffer();
    b.writeLongLong(123);
    disk.saveMaster(b);
    assertEquals(18, disk.totalBytesWritten());
  }
}
