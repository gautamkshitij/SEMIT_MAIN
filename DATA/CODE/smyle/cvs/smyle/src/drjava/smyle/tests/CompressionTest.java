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

import junit.framework.*;
import drjava.smyle.core.*;
import org.artsProject.mcop.*;

public class CompressionTest extends TestCase {
  public CompressionTest(String name) { super(name); }

  int total, totalC;

  public void setUp() {
    total = totalC = 0;
  }

  public void testCompression() {
    testGeneratedData(new Buffer(), 1, 2);
    testGeneratedData(new Buffer(), 35, 6);
    System.out.println("Total: "+total+" -> "+totalC);
  }

  void testGeneratedData(Buffer b, int step, int bytes) {
    testData(b);

    if (bytes > 0) for (int i = 0; i < 256; i += step) {
      Buffer b2 = new Buffer(b);
      b2.writeByte((byte) i);
      testGeneratedData(b2, step, bytes-1);
    }
  }

  void testData(Buffer data) {
    Buffer b = new Buffer(), c = new Buffer();
    Compression.compress(new Buffer(data), b);
    totalC += b.remaining();
    Compression.decompress(new Buffer(b), c);
    if (!data.contentEquals(c))
      fail(data+" -> "+b+" -> "+c);
    total += data.remaining();
  }
}
