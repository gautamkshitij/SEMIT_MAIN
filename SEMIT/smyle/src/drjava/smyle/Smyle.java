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
import drjava.smyle.core.*;

/** Static methods to create and open stores */
public class Smyle {
  /*static {
    System.err.println("Smyle static init, classloader: "+Smyle.class.getClassLoader());
  }*/
  
  /** creates an empty store.
      Warning: If a store already exists in this directory, its contents will be deleted! */
  public static Store createEmptyStore(File dir) {
    return StoreRegistry.createEmptyStore(dir, null);
  }

  /** creates an empty store.  
      Warning: If a store already exists in this directory, its contents will be deleted! */
  public static Store createEmptyStore(String dir) {
    return StoreRegistry.createEmptyStore(new File(dir), null);
  }
  
  /** opens an existing store or creates a new one if it doesn't exist. */
  public static Store openStore(File dir) {
    return StoreRegistry.openStore(dir, null, false);
  }
  
  /** opens an existing store or creates a new one if it doesn't exist. */
  public static Store openStore(String dir) {
    return StoreRegistry.openStore(new File(dir), null, false);
  }
  
  /** opens an existing store in read-only mode. */
  public static Store openStoreReadOnly(File dir) {
    return StoreRegistry.openStore(dir, null, true);
  }
  
  /** opens an existing store in read-only mode. */
  public static Store openStoreReadOnly(String dir) {
    return StoreRegistry.openStore(new File(dir), null, true);
  }

  public static boolean storeExists(File dir) {
    try {
      return new FileSystemDisk(dir, true).getMasterFile() != 0;
    } catch (StoreNotFoundException e) {
      return false;
    }
  }

  public static boolean storeExists(String dir) {
    return storeExists(new File(dir));
  }

  public static boolean storeInUse(File dir) {
    return new FileSystemDisk(dir, true).inUse();
  }

  public static boolean storeInUse(String dir) {
    return storeInUse(new File(dir));
  }

  public static String getVersion() {
    return "0.8.1";
  }
}
