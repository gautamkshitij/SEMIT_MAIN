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

import org.artsProject.mcop.*;
import drjava.smyle.meta.*;

public class UniversalKey implements Comparable<UniversalKey> {
  int[] data;

  public static final UniversalKey
    MIN = new UniversalKey(0x80000000);
  
  public static class MarDemar implements drjava.smyle.core.MarDemar<UniversalKey> {
    public void marshal(Buffer b, UniversalKey key) {
      b.writeLong(key.data.length);
      for (int i = 0; i < key.data.length; i++)
        b.writeLong(key.data[i]);
    }
    
    public UniversalKey read(Buffer b) {
      return new UniversalKey(b);
    }
  }

  public UniversalKey(String s) {
    init(s);
  }
  
  private void init(String s) {
    Buffer b = new Buffer(Buffer.stringToBytes(s));
    while ((b.remaining() % 4) != 0)
      b.writeByte((byte) 0);
    init(b, b.remaining() >> 2);
  }
  
  void init(Buffer b) {
    init(b, b.readLong());
  }
  
  void init(Buffer b, int n) {
    while (n != 0 && b.peekLong((n-1)*4) == 0) --n;
    data = new int[n];
    for (int i = 0; i < n; i++)
      data[i] = b.readLong();
  }
  
  public UniversalKey(int i) {
    init(i);
  }
  
  private void init(int i) {
    Buffer b = new Buffer();
    b.writeLong(i^0x80000000);
    init(b, 1);
  }
  
  public UniversalKey(long l) {
    init(l);
  }
  
  private void init(long l) {
    Buffer b = new Buffer();
    b.writeLongLong(l^0x8000000000000000L);
    init(b, 2);
  }
  
  public UniversalKey(Object o) {
    if (o instanceof String)
      init((String) o);
    else if (o instanceof Integer)
      init(((Integer) o).intValue());
    else if (o instanceof Long)
      init(((Long) o).longValue());
    else if (o instanceof ComparableBoolean)
      init(o == ComparableBoolean.TRUE ? 1 : 0);
    else if (o instanceof Buffer)
      init((Buffer) o);
    else if (o == null)
      init(0x7FFFFFFF);
    else
      throw new RuntimeException("Illegal argument: "+o);
  }

  private UniversalKey(Buffer b, int n) {
    init(b, n);
  }

  private UniversalKey(int[] data, int n) {
    this.data = new int[n];
    System.arraycopy(data, 0, this.data, 0, n);
  }

  private UniversalKey(int[] data) {
    this.data = data;
  }

  /** create a multidimensional key (interleaved) */
  public static UniversalKey mergeDimensions(Object[] os) {
    int n = os.length;
    if (n == 1) 
      if (os[0] instanceof UniversalKey) 
        return (UniversalKey) os[0]; 
      else 
        return new UniversalKey(os[0]);
    UniversalKey[] keys = new UniversalKey[n];
    int len = 0;
    for (int i = 0; i < n; i++) {
      keys[i] = os[i] instanceof UniversalKey ? (UniversalKey) os[i] : new UniversalKey(os[i]);
      len = Math.max(len, keys[i].data.length);
    }
    len *= n;

    Buffer b = new Buffer();
    for (int j = 0, bit = 0; j < len; j++) {
      int x = 0;
      for (int k = 0; k < 32; k++, bit++) {
        int c = bit % n, d = (bit / n) >> 5;
        if (d < keys[c].data.length && (keys[c].data[d] & (1 << (31 - (bit / n) & 31))) != 0)
          x |= 1 << (31 - k);
      }
      b.writeLong(x);
    }	
    return new UniversalKey(b, b.remaining()/4);
  }
  
  /** create a multidimensional key (non-interleaved) */
  public static UniversalKey concatDimensions(Object[] os) {
    int n = os.length;
    UniversalKey[] keys = new UniversalKey[n];
    int len = 0;
    for (int i = 0; i < n; i++) {
      keys[i] = os[i] instanceof UniversalKey ? (UniversalKey) os[i] : new UniversalKey(os[i]);
      //System.out.println("Key length="+keys[i].data.length);
      len += keys[i].data.length;
    }
    int[] data = new int[len];
    int l = 0;
    for (int i = 0; i < n; i++) {
      System.arraycopy(keys[i].data, 0, data, l, keys[i].data.length);
      l += keys[i].data.length;
    }
    return new UniversalKey(data);
  }
  
  public int compareTo(UniversalKey k) {
    return compareTo(k, 0);
  }

  public int compareTo(UniversalKey k, int fromBit) {
    int[] kdata = k.data;
    int l = Math.min(data.length, kdata.length);
    int byt = fromBit >> 5;
    if (byt < l) {
      int mask = (fromBit & 31) == 0 ? -1 : (1 << (32-(fromBit & 31)))-1;
      int a = (data[byt] & mask)^0x80000000, b = (kdata[byt] & mask)^0x80000000;
      if (a < b) return -1; else if (a > b) return 1;

      for (int i = byt+1; i < l; i++) {
        a = data[i]^0x80000000; b = kdata[i]^0x80000000;
        if (a < b) return -1; else if (a > b) return 1;
      }
    }
    //System.out.println(this+"<>"+k+"="+(data.length-l));
    for (int i = l; i <  data.length; i++) if ( data[i] != 0) return  1;
    for (int i = l; i < kdata.length; i++) if (kdata[i] != 0) return -1;
    return 0;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof UniversalKey)) return false;
    return compareTo((UniversalKey) o) == 0;
  }
  
  public String toString() {
    Buffer b = new Buffer();
    for (int i = 0; i < data.length; i++)
      b.writeLong(data[i]);
    return b.toString();
  }

  private static int extract2(int u) {
    return      (u & (1 << 30)) >> 15
              | (u & (1 << 28)) >> 14
              | (u & (1 << 26)) >> 13
              | (u & (1 << 24)) >> 12
              | (u & (1 << 22)) >> 11
              | (u & (1 << 20)) >> 10
              | (u & (1 << 18)) >> 9
              | (u & (1 << 16)) >> 8
              | (u & (1 << 14)) >> 7
              | (u & (1 << 12)) >> 6
              | (u & (1 << 10)) >> 5
              | (u & (1 << 8)) >> 4
              | (u & (1 << 6)) >> 3
              | (u & (1 << 4)) >> 2
              | (u & (1 << 2)) >> 1
              | (u & (1 << 0)); 
  }

  public UniversalKey extractDimension(int i, int n) {
    if (n == 1) return this;
    Buffer b = new Buffer();
    if (n == 2) {
      for (int j = 0; j < data.length; j += 2) {
        int x = (extract2(data[j] >> (1-i)) << 16) 
              | (j+1 < data.length ? extract2(data[j+1] >> (1-i)) : 0);
        b.writeLong(x);
      }
    } else {
      int len = (data.length+n-1)/n;
      for (int j = 0, bit = 0; j < len; j++) {
        int x = 0;
        for (int k = 0; k < 32; k++, bit++) {
          int d = (bit * n + i) >> 5;
          if (d < data.length && (data[d] & (1 << (31 - (bit * n + i) & 31))) != 0)
            x |= 1 << (31 - k);
        }
        b.writeLong(x);
      }	
    }
    return new UniversalKey(b, b.remaining() >> 2);
  }

  public UniversalKey cut(int maxBytes) {
    int max = (maxBytes+3) >> 2;
    //System.out.println("maxBytes="+maxBytes+", len="+data.length);
    if (data.length <= max) return this;
    return new UniversalKey(data, max);
  }

  /** create a key that is greater than or equal to all other keys of that length */
  public static UniversalKey max(int bytes) {
    int n = (bytes+3) >> 2;
    int data[] = new int[n];
    for (int i = 0; i < n; i++) data[i] = 0xFFFFFFFF;
    return new UniversalKey(data);
  }

  /** min-max is a real range
      kMin/kMax is a kind of bitmask (00=must be 0, 11=must be 1, 01=don't care)
  */
  public static boolean inRange(UniversalKey kMin, UniversalKey kMax, 
                                UniversalKey  min, UniversalKey  max) {
    if (min == null) return kMin.compareTo(max) <= 0;
    if (max == null) return kMax.compareTo(min) >= 0;
    int len = Math.max(Math.max(kMin.data.length, kMax.data.length),
                       Math.max( min.data.length,  max.data.length));
    int i;

    // narrowing phase
    for (i = 0; i < len*32; i++) {
      boolean c = min.getBit(i), d = max.getBit(i);
      if (c != d) break;
      if (kMin.getBit(i) == !c && kMax.getBit(i) == !c) return false;
    }

    // comparison phase
    if (kMin.compareTo(max, i) > 0) return false;
    if (kMax.compareTo(min, i) < 0) return false;
    return true;
  }

  public boolean getBit(int i) {
    if (i >= data.length*32) return false;
    return (data[i >> 5] & (1 << (31-(i)))) != 0;
  }
}
