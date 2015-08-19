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
import java.io.*;

/** tests how VM and OS handle file access conflicts */
public class FileHandlingTest extends TestCase {
  public FileHandlingTest(String name) { super(name); }
  
  static final String filename = "temp/somefile";
  
  /** last writer/closer wins */
  public void testInterleavingFileOutputStreams() throws Exception {
    FileOutputStream fos1 = new FileOutputStream(filename);
    fos1.write(1);
    FileOutputStream fos2 = new FileOutputStream(filename);
    fos2.write(2);
    fos1.close();
    fos2.close();

    assertContents(filename, 2);
  }
  
  /** write order matters, not close order */
  public void testInterleavingFileOutputStreamsReverseClose() throws Exception {
    FileOutputStream fos1 = new FileOutputStream(filename);
    fos1.write(1);
    FileOutputStream fos2 = new FileOutputStream(filename);
    fos2.write(2);
    fos2.close();
    fos1.close();

    assertContents(filename, 2);
  }
  
  void assertContents(String filename, int theByte) throws Exception {
    FileInputStream fis = new FileInputStream(filename);
    assertEquals(theByte, fis.read());
    fis.close();
  }
  
  public void testRandomAccessFiles() throws Exception {
    RandomAccessFile raf1 = new RandomAccessFile(filename, "rw");
    RandomAccessFile raf2 = new RandomAccessFile(filename, "rw");
    
    try {
      raf1.writeLong(123L);
      assertEquals(123L, raf2.readLong());
      
      raf2.seek(0);
      raf2.writeLong(456L);
      raf1.seek(0);
      assertEquals(456L, raf1.readLong());
    } finally {
      raf1.close();
      raf2.close();
    }
  }
}
