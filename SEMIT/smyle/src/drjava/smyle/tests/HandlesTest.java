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
import org.artsProject.mcop.*;
import drjava.smyle.core.*;

public class HandlesTest extends TestCase {
  public HandlesTest(String name) { super(name); }
  
  class HString implements Handled<HString> {
    String s;
    Handle<HString> handle;
    
    HString(String s) {
      this.s = s;
    }
    
    public void setHandle(Handle<HString> handle) {
      this.handle = handle;
    }
  }
  
  class HStringMarDemar implements MarDemar<HString> {
    public void marshal(Buffer b, HString s) {
      b.writeString(s.s);
    }
    
    public HString read(Buffer b) {
      return new HString(b.readString());
    }
  }

  Handles<HString> handles;
  MasterChunkManager cm;
  BitSet whiteList;
  HString hRed, hGreen;
  
  public void setUp() throws Exception {
    cm = new DefaultChunkManager(new MemoryDisk(), DiskStore.VERSION);
    handles = new Handles<HString>(cm, new HStringMarDemar());
    whiteList = new BitSet();
    hRed = new HString("red");
    hGreen = new HString("green");
  }
  
  public void testAddHandle() throws Exception {
    Handle<HString> handle = handles.add(hRed);
    assertEquals(handle, hRed.handle);
    assertEquals("red", handle.get().s);
  }
  
  void rotate() throws Exception {
    ChunkRef master = handles.flush();
    BitSet oldWhiteList = (BitSet) whiteList.clone();
    handles.collectChunks(whiteList);
    cm.deleteEverythingBut(whiteList);
    whiteList = oldWhiteList;
    handles = new Handles<HString>(cm, new HStringMarDemar(), master);
  }
  
  public void testSaveAndReadHandles() throws Exception {
    Handle<HString>
      red = handles.add(hRed),
      green = handles.add(hGreen);
    
    // save
    ChunkRef cRed = cm.createChunk(red), cGreen = cm.createChunk(green);
    whiteList.set(cRed.index);
    whiteList.set(cGreen.index);
    
    // load
    rotate();
    green = handles.read(cm.readChunk(cGreen));
    red = handles.read(cm.readChunk(cRed));
    
    // check
    assertEquals("red", red.get().s);
    assertEquals("green", green.get().s);
  }
  
  public void testReplaceHandle() throws Exception {
    Handle<HString> red = handles.add(hRed);
    red.set(new HString("blue"));
    ChunkRef cRed = cm.createChunk(red);
    whiteList.set(cRed.index);
    rotate();
    red = handles.read(cm.readChunk(cRed));
    assertEquals("blue", red.get().s);
    
    red.set(new HString("magenta"));
    rotate();
    red = handles.read(cm.readChunk(cRed));
    assertEquals("magenta", red.get().s);
  }
  
  public void testDisposeLastSlide() throws Exception {
    Handle<HString> red = handles.add(hRed);
    ChunkRef cRed = cm.createChunk(red);
    red.dispose();
    assertNull(hRed.handle);
    try {
      red = handles.read(cm.readChunk(cRed));
      fail("No exception");
    } catch (Exception e) {
      // ok
    }
  }
  
  public void testDisposeSlide() throws Exception {
    Handle<HString> red = handles.add(hRed);
    Handle<HString> green = handles.add(hGreen);
    Buffer b = new Buffer();
    red.marshal(b);
    red.dispose();
    rotate();
    try {
      red = handles.read(b);
      fail("No exception");
    } catch (Exception e) {
      // ok
    }
  }
  
  public void testInvalidate() throws Exception {
    Handle<HString> red = handles.add(hRed);
    handles.flush();
    hRed.s = "blue";
    red.invalidate();
    
    Buffer b = new Buffer();
    red.marshal(b);
    rotate();
    red = handles.read(b);
    assertEquals("blue", red.get().s);
  }
  
  /** it must not be possible to assign two handles to one object */
  // Doesn't work because there's not Handled.getHandle() method */
  /*public void testHandleUniqueness() throws Exception {
    Handle<HString> red = handles.add(hRed);
    
    try {
      handles.add(hRed);
      fail("No exception");
    } catch (Exception e) {
      // ok
    }
    
    Handle<HString> green = handles.add(hGreen);
    try {
      green.set(hRed);
      fail("No exception");
    } catch (Exception e) {
      // ok
    }
  }*/
}
