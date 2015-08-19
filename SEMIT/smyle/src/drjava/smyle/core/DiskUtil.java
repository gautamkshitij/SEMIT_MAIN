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
import org.artsProject.mcop.*;
import drjava.smyle.*;

public class DiskUtil {
  public static Buffer fileToBuffer(Disk disk, FileRef fr) {
    return fileToBuffer(disk, fr.id);
  }
  
  public static Buffer fileToBuffer(Disk disk, long id) {
    try {
      int l = disk.getFileLength(id);
      byte[] data = new byte[l];
      DataInputStream in = new DataInputStream(disk.readFile(id));
      in.readFully(data);
      in.close();
      return new Buffer(data);
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  public static FileRef bufferToFile(Disk disk, Buffer b) {
    Disk.NewFile file = disk.createFile();
    bufferToFile(file, b);
    return new FileRef(file.getId());
  }
  
  public static void bufferToFile(Disk.NewFile file, Buffer b) {
    try {
      OutputStream out = file.getOutputStream();
      byte[] bytes = b.toByteArray();
      // TODO: optimize
      out.write(bytes);
      out.close();
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
}
