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
import drjava.smyle.*;
import drjava.smyle.core.*;
import drjava.util.*;
import org.artsProject.mcop.*;

public class FileSystemDiskTest extends TestCase {
  public FileSystemDiskTest(String name) { super(name); }
  
  final File dir = new File("temp/fsdtest");
  
  FileSystemDisk disk;
  
  public void setUp() throws Exception {
    clearDirectory(dir);
    rotate();
    assert(dir+" exists", dir.isDirectory());
  }
  
  public void tearDown() throws Exception {
    if (disk != null)
      disk.release();
  }
  
  void clearDirectory(File dir) {
    File[] files = dir.listFiles();
    if (files != null) for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory())
        clearDirectory(files[i]);
      else if (!files[i].delete())
        throw new SmyleIOException("Couldn't delete "+files[i]);
    }
  }
  
  void rotate() throws Exception {
    if (disk != null)
      disk.release();
    disk = new FileSystemDisk(dir, false);
  }
  
  long createMasterFile() throws Exception {
    Buffer b = new Buffer();
    b.writeByte((byte) 123);
    return disk.saveMaster(b);
  }

  long createFile() throws Exception {
    Disk.NewFile file = disk.createFile();
    OutputStream out = file.getOutputStream();
    out.close();
    return file.getId();
  }

  void checkMasterFile(long id) throws Exception {
    assertEquals("Master file length", 1, disk.getFileLength(id));
    InputStream in = disk.readFile(id);
    assertEquals(123, in.read());
    in.close();
  }
  
  public void testDeleteEverythingDiscardsMaster() throws Exception {
    createMasterFile();
    disk.deleteEverything();
    assertEquals(0, disk.getMasterFile());
  }
  
  public void testMasterFile() throws Exception {
    disk.deleteEverything();
    
    assertEquals("No master file in empty dir", 0, disk.getMasterFile());

    long id = createMasterFile();    
    assertEquals("Id of first file", 1, id);

    // check file on disk
    
    assert("Files must have an extension", FileSystemDisk.EXT.length() != 0);
    File f = new File(dir, "m"+id+FileSystemDisk.EXT);
    assertEquals("File size on disk", 1, f.length());
    
    assertEquals("Master file", id, disk.getMasterFile());
    checkMasterFile(id);
  }
  
  public void testRetrieveMasterAfterRotate() throws Exception {
    long id = createMasterFile();
    rotate();
    assertEquals("Master file ID", id, disk.getMasterFile());
    checkMasterFile(id);
  }
  
  public void testExceptions() throws Exception {
    disk.deleteEverything();
    
    try {
      // Non existent file
      disk.getFileLength(123);
      fail("should have thrown FileNotFoundException");
    } catch (SmyleIOException e) {
      // ok
    }
    
    try {
      // Non existent file
      disk.readFile(123);
      fail("should have thrown FileNotFoundException");
    } catch (FileNotFoundException e) {
      // ok
    }
  }
  
  public void testForeignFilesAreNotTouched() throws Exception {
    File f = new File(dir, "foreignFile.txt");
    File d = new File(dir, "foreignDir");
    new FileOutputStream(f).close();
    d.mkdir();
    
    disk.deleteEverything();
    
    assertTrue("File is untouched", f.isFile());
    assertTrue("Directory is untouched", d.isDirectory());
  }
  
  public void testTwoMasterFiles() throws Exception {
    disk.deleteEverything();
    
    long master1 = createMasterFile();
    long master2 = createMasterFile();
    assertTrue(master1 != master2);
    
    assertEquals(master2, disk.getMasterFile());
    
    rotate();
    assertEquals(master2, disk.getMasterFile());
  }
  
  public void testNewFileIdsAfterRotate() throws Exception {
    long file1 = createMasterFile();
    rotate();
    long file2 = createMasterFile();
    assertTrue(file2 != file1);
  }
  
  public void testNoDiskId() throws Exception {
    tearDown();
    disk = new FileSystemDisk(dir, 0, 1, false);
    assertEquals(1, createFile());
    assertEquals(2, createFile());
  }
  
  public void testDiskId() throws Exception {
    tearDown();
    disk = new FileSystemDisk(dir, 1234, 10000, false);
    assertEquals(11234, createFile());
    assertEquals(21234, createFile());
  }
  
  public void testHugeId() throws Exception {
    tearDown();
    disk = new FileSystemDisk(dir, 1234, 10000, false);
    disk.setIdCounter(0x100000000L);
    assertEquals(0x100000001L*10000+1234, createFile());
  }
  
  public void testDiskIsSynchronized() throws Exception {
    // include package private methods to check for calls from inner classes
    SynchronisationTester.assertPackageAndPublicMethodsAreSynchronized(disk.getClass());
  }
  
  /** We just assume this works across processes too (no tricks with
      static variables or global syncing) */
  public void testDiskInUse() throws Exception {
    disk = null;
    try {
      rotate();
      fail("No exception");
    } catch (DiskInUseException e) {
      // ok
    }
  }
  
  public void testDiskInUseFlagClearedOnFinalization() throws Exception {
    disk = null;
    System.gc();
    System.runFinalization();
    rotate();
  }

  public void testNumberOfFiles() throws Exception {
    // TODO
  }
  
  public void testDeleteEverythingClosesOpenFiles() throws Exception {
    disk.createFile();
    disk.deleteEverything();
  }
  
  public void testReleasesCloseOpenFiles() throws Exception {
    disk.createFile();
    rotate();
    disk.deleteEverything();
  }
}
