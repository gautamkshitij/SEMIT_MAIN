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

public interface Disk {
  public interface NewFile {
    public long getId();
    public OutputStream getOutputStream() throws IOException;
  }
  
  public NewFile createFile();
  
  /** atomically create and fill master file */
  public long saveMaster(Buffer data);
  
  /** returns the current master file id, or 0 if there is no master file */
  public long getMasterFile();
  
  public InputStream readFile(long id) throws IOException;
  public int getFileLength(long id);

  public void deleteEverything();
  public void deleteEverythingBut(Set<FileRef> whiteList);
  
  /** for calculation of totalBytesWritten() */
  public void setClusterSize(int bytes);
  public long totalBytesWritten();
  
  /** state that disk object isn't needed anymore; clear locks if any */
  public void release();
}
