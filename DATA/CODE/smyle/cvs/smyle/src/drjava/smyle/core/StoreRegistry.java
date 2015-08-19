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
import java.lang.ref.*;
import drjava.smyle.*;

public class StoreRegistry {
  private static final HashMap<String,WeakReference<DiskStore>> stores
    = new HashMap<String,WeakReference<DiskStore>>();
    
  public static synchronized Store createEmptyStore(File dir, Class cls) {
    DiskStore store = findStore(dir);
    if (store != null)
      throw new BadUseException("Store is already open, can't create");
    else {
      FileSystemDisk disk = new FileSystemDisk(dir, false);
      disk.deleteEverything();
      store = createStore(disk, cls, false);
      addStore(dir, store);
      return store;
    }
  }
  
  public static synchronized Store openStore(File dir, Class cls, boolean readOnly) {
    DiskStore store = findStore(dir);
    if (store != null) {
      store.addReference();
      return store;
    } else {
      store = createStore(new FileSystemDisk(dir, readOnly), cls, readOnly);
      addStore(dir, store);
      return store;
    }
  }
  
  static private DiskStore createStore(Disk disk, Class cls, boolean readOnly) {
    if (cls == null) return new DiskStore(disk, readOnly);
    try {
      return (DiskStore) cls.getConstructor(new Class[] { Disk.class, Boolean.TYPE })
        .newInstance(new Object[] { disk, readOnly ? Boolean.TRUE : Boolean.FALSE });
    } catch (Exception e) {
      e.printStackTrace();
      throw new SmyleIOException(e.toString());
    }
  }

  static synchronized void removeStore(DiskStore store) {
    Collection<WeakReference<DiskStore>> c = stores.values();
    for (Iterator<WeakReference<DiskStore>> i = c.iterator(); i.hasNext(); ) {
      WeakReference<DiskStore> ref = i.next();
      if (ref.get() == store) {
        i.remove();
        break;
      }
    }
  }
  
  static void addStore(File dir, DiskStore store) {
    store.enableIndexing();
    try {
      stores.put(dir.getCanonicalPath(), new WeakReference<DiskStore>(store));
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
  
  static DiskStore findStore(File dir) {
    try {
      String path = dir.getCanonicalPath();
      WeakReference<DiskStore> ref = stores.get(path);
      if (ref == null) return null;
      DiskStore store = ref.get();
      if (store == null)
        stores.remove(path);
      return store;
    } catch (IOException e) {
      throw new SmyleIOException(e);
    }
  }
}
