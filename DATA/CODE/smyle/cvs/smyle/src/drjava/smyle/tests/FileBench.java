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
import java.util.*;
import drjava.util.*;
import drjava.smyle.*;
import drjava.smyle.core.*;
import org.artsProject.mcop.*;

public class FileBench extends Benchmark {
  static final int n = 100000, repeat = 3;
  FileSystemDisk disk;
  
  public FileBench() {
    setDescription("Creating and randomly accessing "+n+" files");
  }
  
  protected void action() {
    disk = new FileSystemDisk(new File("temp/filebench"), false);
    
    for (int i = 0; i < n; i++) {
      Buffer buf = new Buffer();
      buf.writeLong(1);
      DiskUtil.bufferToFile(disk, buf);
    }
    done("Create "+n+" files");
    
    Random rand = new Random(0);
    
    for (int r = 0; r < repeat; r++) {
      for (int i = 0; i < n; i++) {
        int fileNr = ((rand.nextInt() & 0x7FFFFFFF) % n)+1;
        DiskUtil.fileToBuffer(disk, fileNr);
      }
      done("Access "+n+" files");
    }
    disk.release();
    done("release");
  }
  
  public static void main(String[] args) {
    for (int i = 0; i < 1; i++) {
      FileBench bench = new FileBench();
      bench.runAndPrint();
    }
  }
}
