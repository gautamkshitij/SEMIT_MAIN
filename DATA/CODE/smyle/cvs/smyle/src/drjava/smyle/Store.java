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

package drjava.smyle;

import java.io.*;
import org.artsProject.mcop.*;

/** A Smyle store */
public interface Store {
  /** creates an immutable snapshot */
  public Snapshot snapshot();
  
  /** creates a mutable snapshot.
      Throws a TimeoutException if the write lock isn't being released
      after the time specified in setTimeout() */
  public Snapshot mutableSnapshot() throws TimeoutException;
  
  /** closes the store */
  public void close();
  
  /** sets the time that mutableSnapshot() will wait for the write lock to be released.
      Default value is 0 (no timeout, wait infinitely) */
  public void setTimeout(int ms);

  /** WARNING: Any open snapshots become invalid when you do this */
  //public void deleteEverything();
  
  /** Specify where to print log messages (warnings, further explanations
      on exceptions).
      Default is System.err. A null value is allowed (turns off logging)
      */
  public void logTo(PrintWriter writer);
  
  /** returns true if write locking is exclusive - that is, if there can only be one
      mutable snapshot at any time */
  public boolean exclusiveWriteLocking();
  
  /** determines how often a garbage collection should take place
      @param bytes the number of bytes that must be written before
      a GC is performed */
  public void setGCFrequency(int bytes);
  
  /** informs Smyle about the OS's assumed cluster size
      - GC frequency is calculated
      more exactly if cluster size is set correctly */
  public void setClusterSize(int bytes);
  
  /** optimize store on disk (relocate data to minimize seeks) */
  public void optimize();
  
  /** clear all caches
      You should call System.gc() afterwards for best results */
  public void clearCaches();
}
