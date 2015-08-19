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
import org.artsProject.mcop.*;

public class Handles<T implements Handled<T>> {
  ChunkManager cm;
  MarDemar<T> md;
  ArrayList<HandleImpl> handles = new ArrayList<HandleImpl>();
  ChunkRef master;
  int firstEmptyHandle = 0;
  
  class HandleImpl implements Handle<T> {
    int index;
    T t;
    ChunkRef chunk;
    
    HandleImpl(T t, int index) {
      this.t = t;
      this.index = index;
      t.setHandle(this);
    }
    
    HandleImpl(ChunkRef chunk, int index) {
      this.chunk = chunk;
      this.index = index;
    }
    
    public T get() {
      if (t == null) {
        t = md.read(cm.readChunk(chunk));
        t.setHandle(this);
      }
      return t;
    }
    
    public void marshal(Buffer b) {
      b.writeLong(index);
    }
    
    ChunkRef getChunk() {
      if (chunk == null) {
        Buffer b = new Buffer();
        md.marshal(b, t);
        chunk = cm.createChunk(b);
      }
      return chunk;
    }
    
    public void set(T t) {
      this.t = t;
      chunk = null;
      master = null;
      t.setHandle(this);
    }
    
    public void dispose() {
      if (t != null)
        t.setHandle(null);
      handles.set(index, null);
      if (firstEmptyHandle > index) firstEmptyHandle = index;
      while (handles.size() != 0 && handles.get(handles.size()-1) == null)
        handles.remove(handles.size()-1);
      if (firstEmptyHandle > handles.size()) firstEmptyHandle = handles.size();
      master = null;
    }
    
    public void invalidate() {
      chunk = null;
      master = null;
    }
  }
  
  public Handles(ChunkManager cm, MarDemar<T> md) {
    this.cm = cm;
    this.md = md;
  }
  
  public Handles(ChunkManager cm, MarDemar<T> md, ChunkRef chunk) {
    this(cm, md);
    Buffer b = cm.readChunk(master = chunk);
    int n = b.readLong();
    for (int i = 0; i < n; i++) {
      ChunkRef c = new ChunkRef(b);
      if (c.index != 0)
        handles.add(new HandleImpl(c, i));
      else
        handles.add(null);
    }
  }
  
  public Handle<T> add(T t) {
    int i;
    for (i = firstEmptyHandle; i < handles.size(); i++)
      if (handles.get(i) == null)
        break;
    firstEmptyHandle = i+1;
    HandleImpl s = new HandleImpl(t, i);
    if (i < handles.size())
      handles.set(i, s);
    else
      handles.add(s);
    master = null;
    return s;
  }
  
  public Handle<T> read(Buffer b) {
    HandleImpl h = handles.get(b.readLong());
    if (h == null)
      throw new RuntimeException("Handle was disposed");
    return h;
  }
  
  public ChunkRef flush() {
    if (master == null) {
      Buffer b = new Buffer();
      b.writeLong(handles.size());
      for (int i = 0; i < handles.size(); i++) {
        HandleImpl h = handles.get(i);
        if (h != null)
          h.getChunk().writeType(b);
        else
          ChunkManager.NULLCHUNK.writeType(b);
      }
      master = cm.createChunk(b);
    }
    return master;
  }
  
  public void collectChunks(BitSet whiteList) {
    for (int i = 0; i < handles.size(); i++) {
      HandleImpl h = handles.get(i);
      if (h != null) {
        ChunkRef chunk = handles.get(i).chunk;
        if (chunk != null)
          whiteList.set(chunk.index);
      }
    }
    if (master != null)
      whiteList.set(master.index);
  }
  
  public Handle<T> fixedIndex(int index) {
    return handles.get(index);
  }
}
