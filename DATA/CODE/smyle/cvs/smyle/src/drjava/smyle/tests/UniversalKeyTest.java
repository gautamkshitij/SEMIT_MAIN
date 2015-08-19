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
import junit.framework.*;
import org.artsProject.mcop.*;
import drjava.smyle.core.*;
import drjava.util.*;

public class UniversalKeyTest extends TestCase {
  public UniversalKeyTest(String name) { super(name); }
  
  public void testComparison() throws Exception {
    assertEquals(0, new UniversalKey(0).compareTo(new UniversalKey(0)));
    assertTrue(new UniversalKey(0xff).compareTo(new UniversalKey(0)) > 0);
    assertTrue(new UniversalKey(0x80).compareTo(new UniversalKey(0)) > 0);
    assertTrue(new UniversalKey(-1).compareTo(new UniversalKey(0xff)) < 0);
    assertTrue(new UniversalKey(-1L).compareTo(new UniversalKey(1L)) < 0);
    assertTrue(new UniversalKey(0xffffffffL).compareTo(new UniversalKey(0L)) > 0);
  }
  
  public void testStringComparison() throws Exception {
    UniversalKey last = null;
    for (int i = 0; i < 65536; i += 16) {
      if (i < 0xD800 || i >= 0xDC00) {
        UniversalKey k = new UniversalKey(String.valueOf((char) i));
        if (last != null)
          assertTrue("i="+i, k.compareTo(last) > 0);
        last = k;
      }
    }
  }

  public void testMergeAndExtractDimensions() throws Exception {
    assertEquals("80004400", UniversalKey.mergeDimensions(new Object[] { new Integer(0x4400) }).toString());
    assertEquals("C000000020200404", UniversalKey.mergeDimensions(new Object[] { 
      new Integer(0x4400), new Integer(0x0022) }).toString());
    UniversalKey k = UniversalKey.mergeDimensions(new Object[] { 
      new Integer(0xC4000000), new Integer(0x7F000000) });
    assertEquals("75750000", k.toString());
    assertEquals("44000000", k.extractDimension(0, 2).toString());
    assertEquals("FF000000", k.extractDimension(1, 2).toString());
  }

  public void testMinMax() throws Exception {
    assertTrue(new UniversalKey(0x80).compareTo(UniversalKey.max(1)) < 0);
    assertTrue(new UniversalKey(0x80).compareTo(UniversalKey.MIN) > 0);

    assertEquals("8000000020200000", UniversalKey.mergeDimensions(new Object[] { 
      new Integer(0x4400), UniversalKey.MIN }).toString());
    assertEquals("D555555575755555", UniversalKey.mergeDimensions(new Object[] { 
      new Integer(0x4400), UniversalKey.max(4) }).toString());
  }

  public void testCut() throws Exception {
    UniversalKey key = new UniversalKey(new Buffer("000000020000000112344321"));
    assertTrue(key == key.cut(12));
    assertTrue(key == key.cut(8));
    assertEquals("00000001", key.cut(4).toString());
  }

  public void testGetBit() throws Exception {
    UniversalKey key = new UniversalKey(new Buffer("000000020000000112344321"));
    assertEquals(false, key.getBit(32+2));
    assertEquals(true,  key.getBit(32+3));
  }
  
  public void testConcatDimensions() throws Exception {
    assertEquals("80004400", UniversalKey.concatDimensions(new Object[] { new Integer(0x4400) }).toString());
    assertEquals("8000440080000022", UniversalKey.concatDimensions(new Object[] { 
      new Integer(0x4400), new Integer(0x0022) }).toString());
    /*UniversalKey k = UniversalKey.mergeDimensions(new Object[] { 
      new Integer(0xC4000000), new Integer(0x7F000000) });
    assertEquals("75750000", k.toString());
    assertEquals("44000000", k.extractDimension(0, 2).toString());
    assertEquals("FF000000", k.extractDimension(1, 2).toString());*/
  }
}
