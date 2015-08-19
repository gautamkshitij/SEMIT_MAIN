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

/** stores individual bits in a byte buffer and reads them back again.
    You should only use each BitBuffer instance for reading OR writing. */
public class BitBuffer {
  Buffer buffer;
  Buffer offstream;
  int b, bit, rbit = 8;
  
  public BitBuffer(Buffer buffer) {
    this.buffer = buffer;
  }
  
  public void writeBits(int data, int len) {
    while (bit+len >= 8) {
      buffer.writeByte((byte) (b | (data << bit)));
      flushOffstream();
      data >>= 8-bit;
      len -= 8-bit;
      bit = b = 0;
    }
    
    b |= data << bit;
    bit += len;
  }
  
  void flushOffstream() {
    if (offstream != null) {
      buffer.writeBuffer(offstream);
      offstream = null;
    }
  }
  
  public void writeOffstreamByte(byte b) {
    if (bit == 0)
      buffer.writeByte(b);
    else {
      if (offstream == null)
        offstream = new Buffer();
      offstream.writeByte(b);
    }
  }
  
  public void close() {
    if (bit != 0) {
      buffer.writeByte((byte) b);
      bit = b = 0;
    }
    flushOffstream();
  }
  
  /** returns the bits or -1 if end of stream is reached */
  public int readBits(int len) {
    // fetch new byte if necessary
    if (rbit == 8) {
      if (buffer.remaining() == 0) return -1;
      b = buffer.readByte() & 0xFF;
      rbit = 0;
    }
    
    int n = 0, bb = 0;
    while (rbit+len > 8) {
      n |= (b >> rbit) << bb;
      b = buffer.readByte() & 0xFF;
      bb += 8-rbit;
      len -= 8-rbit;
      rbit = 0;
    }
    n |= ((b >> rbit) & ((1 << len)-1)) << bb;
    rbit += len;
    return n;
  }
  
  /** returns a byte (0..255) or -1 if end of stream is reached */
  public int readOffstreamByte() {
    if (buffer.remaining() == 0) return -1;
    return buffer.readByte() & 0xFF;
  }
}
