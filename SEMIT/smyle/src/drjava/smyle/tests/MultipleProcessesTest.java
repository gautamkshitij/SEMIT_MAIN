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

import junit.framework.*;
import drjava.smyle.*;
import drjava.smyle.core.*;

/** This test is disabled for the time being (multiple process access
    isn't allowed) */
public class MultipleProcessesTest extends TestCase {
  public MultipleProcessesTest(String name) { super(name); }
  
  static final String dir = "temp/mptest";
  
  FileSystemDisk disk1, disk2;
  DiskStore store1, store2;
  
  protected void setUp() throws Exception {
    /*disk1 = new FileSystemDisk(dir);
    disk1.deleteEverything();
    store1 = new DiskStore(disk1);
    
    disk2 = new FileSystemDisk(dir);
    store2 = new DiskStore(disk2);*/
  }
  
  protected void tearDown() throws Exception {
    if (store1 != null) store1.close();
    if (store2 != null) store2.close();
  }
  
  /** let both disks create a file and check that ids are different
      (a very basic test case, but it failed at the time it was written) */
  public void testNoFileConflicts() throws Exception {
    /*Disk.NewFile file1 = disk1.createFile();
    Disk.NewFile file2 = disk2.createFile();
    assertTrue("File id "+file1.getId()+" used by both processes", file1.getId() != file2.getId());*/
  }
}
