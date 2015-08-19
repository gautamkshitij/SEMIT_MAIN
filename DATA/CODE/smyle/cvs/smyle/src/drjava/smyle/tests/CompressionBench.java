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

import java.io.*;
import java.util.*;
import java.util.zip.*;
import drjava.util.*;
import drjava.smyle.*;
import drjava.smyle.core.*;

public class CompressionBench extends Benchmark {
  Disk disk;
  DefaultChunkManager chunkManager;
  ArrayList<byte[]> chunks = new ArrayList<byte[]>();
  int totalBytes, totalCompressed;
  byte[] chunk;
  int l; // length of chunk
  int[] optimal, used, power, saved;
  
  public CompressionBench() {
    disk = new FileSystemDisk(new File("etc/compression"), true);
    //System.out.println("Master: "+disk.getMasterFile());
    chunkManager = new DefaultChunkManager(disk, 281);
    chunkManager.getMasterChunk(); // needed to read chunk table
    
    for (Iterator<ChunkRef> i = chunkManager.chunks1(); i.hasNext(); ) {
      byte[] b = chunkManager.readChunk(i.next()).toByteArray();
      chunks.add(b);
      totalBytes += b.length;
    }
    disk.release();
    
    setDescription("Compress "+chunks.size()+" chunks ("+totalBytes+" bytes)");
  }
  
  int deflate() {
    byte[] buf = new byte[l+512];
    Deflater deflater = new Deflater();
    deflater.setInput(chunk);
    deflater.finish();
    int compressed = deflater.deflate(buf);
    if (compressed >= buf.length)
      throw new RuntimeException("Buffer overflow");
    return compressed;
  }
  
  int gzip() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream out = new GZIPOutputStream(baos);
      out.write(chunk);
      out.close();
      return baos.size();
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }
  
  int noCompression() {
    return l;
  }
  
  /** looks for sequences of zeros and non-zeros */
  int countZeros() {
    int n = 0;
    for (int i = 0; i < l; ) {
      int zeros = 0, nonzeros = 0;
      while (zeros < 3 && i+zeros < l && chunk[i+zeros] == 0) ++zeros;
      i += zeros;
      
      // basic (stop at first zero)
      while (nonzeros < 63 && i+nonzeros < l && chunk[i+nonzeros] != 0) ++nonzeros;
      
      // advanced (skip one zero)
      /*boolean z = false;
      while (nonzeros < 63 && i+nonzeros < l)
        if (chunk[i+nonzeros] != 0) ++nonzeros;
        else if (z) break; // found 2nd zero
        else z = true;     // found 1st zero
      // push back first zero if the two zeros were consecutive
      // and we're not at the end of the stream
      if (nonzeros > 0 && i+nonzeros < l && chunk[i+nonzeros-1] == 0) --nonzeros;*/
      
      i += nonzeros;
      n += 1+nonzeros;
    }
    return n;
  }
  
  /*int highestBit() {
    int bits = 0;
    for (int i = 0; i < l; i++) {
      bits += chunk[i] < 0 ? 9 : 7;
    }
    return (bits+7)/8;
  }*/
  
  int diff(int diffBits) {
    int bits = 8;
    for (int i = 1; i < l; i++) {
      int d = chunk[i]-chunk[i-1];
      bits += (d == (d & ((1 << diffBits)-1))) ? 1+diffBits : 1+8;
    }
    return (bits+7)/8;
  }
  
  /** combines one or more algorithms and chooses the best method
      for each chunk (including no compression).
      needs an additional byte to record which method was chosen */
  int chooseBest(int[] results) {
    if (optimal == null) {
      optimal = new int[results.length];
      used = new int[results.length];
      power = new int[results.length];
      saved = new int[results.length];
    }
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < results.length; i++)
      if (results[i] < best) best = results[i];
    for (int i = 0; i < results.length; i++)
      if (results[i] == best) {
        ++optimal[i];
        power[i] += l-best-1;
      }
    for (int i = 0; i < results.length; i++)
      if (results[i] == best) {
        ++used[i];
        saved[i] += l-best-1;
        break;
      }
    return best+1;
  }
  
  protected void action() {
    totalCompressed = 0;
    optimal = used = saved = null;
    for (int i = 0; i < chunks.size(); i++) {
      chunk = chunks.get(i);
      l = chunk.length;
      
      int compressed = chooseBest(new int[] {
        noCompression(),
        Compression.estimateHighNibble(chunk, 0, 2, 6),
        ///highNibble(0, 2, 7),
        Compression.estimateHighNibble(chunk, 0, 6, 7),
        Compression.estimateHighNibble(chunk, 2, 6, 7),
        Compression.estimateZeros(chunk),
        Compression.estimateDoubleZeros(chunk),
        //countZeros(),
        //highestBit(),
        //diff(1),
        //diff(2),
        //diff(3),
        Compression.estimateXOR(chunk, 1),
        //xor(2),
        //xor(3), 
        //xor(4), 
        Compression.estimateXOR(chunk, 5),
        ///deflate(),
        ///gzip(), // it seems gzip is always worse than deflate
      });
      
      //System.out.println(chunk.length+" -> "+compressed);
      totalCompressed += compressed;
    }
  }
  
  void printInfo() {
    System.out.println("Compressed size: "+totalCompressed
      +" ("+(totalCompressed*1000L/totalBytes)*0.1f+"%)");
    for (int i = 0; i < optimal.length; i++)
      System.out.println(optimal[i]+" "+used[i]+" "+" "+power[i]+" "+saved[i]);
  }
  
  public static void main(String[] args) {
    CompressionBench bench = new CompressionBench();
    for (int i = 0; i < 1; i++)
      bench.runAndPrint();
    bench.printInfo();
    //System.out.println("Records/s: "+(long) (n/(bench.totalTime()*0.001)));
    //System.gc(); // for memprofile; store is closed, but still referenced
  }
}
