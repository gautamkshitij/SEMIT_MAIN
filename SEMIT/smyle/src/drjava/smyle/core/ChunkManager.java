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

public abstract class ChunkManager {
  public abstract Buffer readChunk(int chunk);
  public abstract ChunkRef createChunk(Buffer data);
  
  // convenience methods
  
  public synchronized ChunkRef createChunk(Marshallable data) {
    Buffer b = new Buffer();
    data.marshal(b);
    return createChunk(b);
  }
  
  public synchronized Buffer readChunk(ChunkRef chunk) {
    return readChunk(chunk.index);
  }
  
  public static final ChunkRef NULLCHUNK = new ChunkRef(0);
}
