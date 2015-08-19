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

import java.util.*;
import java.io.*;
import org.artsProject.mcop.*;

public class IndexAdvisor<F> {
  F indexedField;
  HashMap<F,Long> fieldStats;
  //MarDemar<F> md;
  long initialHurdle = DEFAULT_INITIAL_HURDLE;
  boolean frozen = false;
  
  static final long DEFAULT_INITIAL_HURDLE = 5000;
  
  public IndexAdvisor(/*MarDemar<F> md*/) {
    //this.md = md;
    fieldStats = new HashMap<F,Long>();
  }
  
  /*public IndexAdvisor(MarDemar<F> md, ChunkManager cm, ChunkRef cr) {
    this(md);
    IAStats stats = new IAStats(cm.readChunk(cr));
    for (int i = 0; i < stats.fieldStats.size(); i++) {
      IAFieldStats fs = stats.fieldStats.get(i);
      fieldStats.put(md.read(new Buffer(fs.field.toArray())), new Long(fs.records));
    }
  }*/
  
  /*public ChunkRef saveStats(ChunkManager cm) {
    IAStats stats = new IAStats();
    for (Iterator<Map.Entry<F,Long>> i = fieldStats.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<F,Long> entry = i.next();
      IAFieldStats fs = new IAFieldStats(entry.getValue().longValue());
      Buffer b = new Buffer();
      md.marshal(b, entry.getKey());
      fs.field.set(b.toByteArray());
      stats.fieldStats.add(fs);
    }
    return cm.createChunk(stats);
  }*/
  
  public void collectChunks(BitSet whiteList, ChunkRef chunk) {
    whiteList.set(chunk.index);
  }
  
  /** returns the name of the field that should be indexed;
      or null if no indices are needed */
  public F fieldToIndex() {
    if (frozen) return indexedField;
    
    // find field with greatest value
    F bestField = null;
    long max = 0L;
    for (Iterator<Map.Entry<F,Long>> i = fieldStats.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<F,Long> entry = i.next();
      if (entry.getValue().longValue() > max) {
        max = entry.getValue().longValue();
        bestField = entry.getKey();
      }
    }
    
    // check if it is worthwhile to replace current index
    /*System.out.println("current: "+indexedField+", new: "+bestField
      +", value="+max+", hurdle: "+hurdle());*/
    if (bestField != null && max >= hurdle())
      return bestField;
    else
      return indexedField;
  }
  
  /** used by store to notify index advisor that an index has been built */
  public void setIndexedField(F field) {
    indexedField = field;
  }
  
  /** minimum value a new fieldToIndex must have */
  long hurdle() {
    if (indexedField != null)
      return getStats(indexedField)*2;
    else
      return initialHurdle;
  }
  
  /** notifies the advisor that a query was performed
      @param records the number of records that were read unnecessarily */
  // TODO: does the first argument need to be an array?
  public void queryPerformed(F[] fields, long records) {
    for (int i = 0; i < fields.length; i++)
      increaseStats(fields[i], records);
  }
  
  void increaseStats(F field, long n) {
    setStats(field, getStats(field)+n);
  }
  
  public long getStats(F field) {
    Long L = fieldStats.get(field);
    return L != null ? L.longValue() : 0L;
  }
  
  void setStats(F field, long n) {
    fieldStats.put(field, new Long(n));
  }
  
  public void setInitialHurdle(long h) {
    initialHurdle = h;
  }
  
  public void freeze() {
    frozen = true;
  }
  
  public String toString() {
    return fieldStats.toString();
  }
  
  public Iterator<F> fields() {
    return fieldStats.keySet().iterator();
  }
}
