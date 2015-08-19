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

import drjava.util.*;
import drjava.smyle.*;
import drjava.smyle.testtypes.*;

public class SmyleBench extends Benchmark {
  static final int n = 250000, commitFrequency = 1000000;
  Store store;
  Snapshot snapshot;
  Table<Person> table;
  
  public SmyleBench() {
    setDescription("Adding, indexing and removing "+n+" Person records");
  }
  
  void commit() {
    snapshot.commitAndContinue();
    /*snapshot.commit();
    snapshot = store.mutableSnapshot();
    table = snapshot.getTable("people", Person.getTypeInfo());*/
  }
  
  void add() {
    for (int i = 0; i < n; i++) {
      table.add(new Person(String.valueOf(i), i));
      //table.remove(0);
      if (i % commitFrequency == commitFrequency-1)
        commit();
    }
    done("add "+n+" records");
  }

  void removeFromTop() {  
    for (int i = 0; i < n; i++)
      table.remove(0);
    done("remove all records from top");
  }
        
  void removeFromBottom() {  
    for (int i = n-1; i >= 0; i--)
      table.remove(i);
    done("remove all records from bottom");
  }
        
  void removeFromTopFilter() {  
    for (int i = 0; i < n; i++) {
      table.removeAll(new Person_filter().ageEquals(i));
      if (table.size() != n-i-1)
        throw new RuntimeException("Wrong table size (expected "+(n-i-1)+", was "+table.size()+")");
    }
    done("remove all records from top (through filter)");
  }
        
  void removeFromTopByStringField() {  
    for (int i = 0; i < n; i++) {
      table.removeAll(new Person_filter().nameEquals(String.valueOf(i)));
      if (table.size() != n-i-1)
        throw new RuntimeException("Wrong table size (expected "+(n-i-1)+", was "+table.size()+")");
    }
    done("remove all records from top (filter on string field)");
  }
        
  void removeFromTopByTwoFields() {  
    for (int i = 0; i < n; i++) {
      table.removeAll(i % 2 == 0 ? new Person_filter().ageEquals(i) 
		  		 : new Person_filter().nameEquals(String.valueOf(i)));
      if (table.size() != n-i-1)
        throw new RuntimeException(i+": wrong table size (expected "+(n-i-1)+", was "+table.size()+")");
    }
    done("remove all records from top (through alternating filters)");
  }
        
  protected void action() {
    store = Smyle.createEmptyStore("temp/bench");
    store.setGCFrequency(1000000);
    try {
      ((drjava.smyle.core.DiskStore) store).enableIndexing();
      snapshot = store.mutableSnapshot();
      table = snapshot.getTable("people", Person.getTypeInfo());
      
      add();
      //System.gc(); System.exit(0);
      //removeFromTop();
      //removeFromBottom();
      snapshot.commitAndContinue();
      done("commit");
      removeFromTopFilter();
      //removeFromTopByStringField();
      //removeFromTopByTwoFields();
      
      //System.gc();
      //System.exit(0); // highest mem usage should be about here
      
      snapshot.commit();
      done("commit");
    } finally {
      store.close();
      done("close store");
    }
  }
  
  public static void main(String[] args) {
    for (int i = 0; i < 1; i++) {
      SmyleBench bench = new SmyleBench();
      bench.runAndPrint();
      System.out.println("Records/s: "+(long) (n/(bench.totalTime()*0.001)));
    }
    System.gc(); // for memprofile; store is closed, but still referenced
  }
}
