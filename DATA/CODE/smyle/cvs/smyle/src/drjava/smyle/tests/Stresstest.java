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

import java.util.*;
import junit.framework.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;
import drjava.smyle.core.*;

public class Stresstest extends Assert {
  // immutables currently force frequent flushing
  static final boolean immutables = false;

  static class Worker extends Thread {
   Store store;
   String tableName;

   Worker(Store store, String tableName) { 
     this.store = store;
     this.tableName = tableName; 
   }

   public void run() {
    try {
    while (true) {
     
     for (int i = 0; i < 300; i++) {
      //System.out.println(i);
      
      if (immutables)
        store.snapshot(); // just get and leave open

      Snapshot snapshot = store.mutableSnapshot();
      Table<StressRecord> table = snapshot.getTable(tableName, StressRecord.getTypeInfo());
      if (i == 0) table.clear();
      
      for (int key2 = 0; key2 < 100; key2++) {
        StressRecord r = table.get(
          new StressRecord_filter().key1Equals("").key2Equals(key2));
        if (key2 >= i)
          assertNull(r);
        else
          assertTrue("key2="+key2+", r="+r, r != null && r.key2 == key2
            /*&& r.value == (i-1)/100*100+key2*/);
      }
      
      //int key2 = (random.nextInt() & 0x7fffffff) % 100;
      int key2 = i % 100;
      StressRecord_filter filter = 
        new StressRecord_filter().key1Equals("").key2Equals(key2);
      int idx = table.indexOf(filter);
      if (idx >= 0) {
        //System.out.println("update "+key2+" to "+i);
        table.set(idx, table.get(idx).value(i));
      } else {
        //System.out.println("add "+key2+"/"+i);
        table.add(new StressRecord()
          .key1("").key2(key2).value(i));
      }
      
      /*PersistentBTree index = ((TableImpl<StressRecord>) table).getIndex();
      if (index != null)
        BTreeTestBase.printTree(index);*/
          
      snapshot.commit();
     }
     System.out.println(tableName+": cycle complete");
    }
   } catch (Throwable e) {
    e.printStackTrace();
    System.exit(1);
   }
   }
  }

  public static void main(String[] args) throws Exception {
    Store store = Smyle.createEmptyStore("temp/stress");
    //((drjava.smyle.core.DiskStore) store).enableIndexing();
    //store.setWriteLatency(200);
    DiskStore.showGC = false;
    DefaultChunkManager.verbose = false;

    Random random = new Random(0);
    
    for (int i = 1; i <= 5; i++) {
      new Worker(store, "table"+i).start();
    }
    
    //store.close();
  }
}
