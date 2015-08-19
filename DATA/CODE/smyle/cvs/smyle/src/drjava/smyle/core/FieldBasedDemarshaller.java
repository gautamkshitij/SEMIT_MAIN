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
import org.artsProject.util.*;
import drjava.smyle.*;
import drjava.smyle.meta.*;

class FieldBasedDemarshaller<T extends Struct<T>> implements Demarshaller<T> {
  TypeInfo<T> typeInfo;
  ArrayList<FieldReader> fieldReaders = new ArrayList<FieldReader>();
  
  abstract class FieldReader {
    abstract void readField(Buffer buffer, T t);
  }
  
  FieldBasedDemarshaller(TypeInfo<T> typeInfo) {
    this.typeInfo = typeInfo;
  }
  
  void addFieldReader(FieldReader fr) {
    fieldReaders.add(fr);
  }
  
  void addLongField(final int nr) {
    class LongReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        int v = buffer.readLong();
        if (nr >= 0) t.setField(nr, new Integer(v));
      }
    }
    fieldReaders.add(new LongReader());
  }
  
  void addLongLongField(final int nr) {
    class LongLongReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        long v = buffer.readLongLong();
        if (nr >= 0) t.setField(nr, new Long(v));
      }
    }
    fieldReaders.add(new LongLongReader());
  }
  
  void addBooleanField(final int nr) {
    class BooleanReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        boolean v = buffer.readBoolean();
        if (nr >= 0) t.setField(nr, v ? ComparableBoolean.TRUE : ComparableBoolean.FALSE);
      }
    }
    fieldReaders.add(new BooleanReader());
  }
  
  void addStringField(final int nr) {
    class StringReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        String v = buffer.readString();
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new StringReader());
  }
  
  <A extends Struct<A>> void addStructField(final int nr, final TypeInfo<A> type) {
    class StructReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        A v = type.demarshaller.read(buffer);
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new StructReader());
  }
  
  <A> void addSequenceField(final int nr, final Demarshaller<A> demarshaller) {
    class SequenceReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        ArrayList<A> v = new ArrayList<A>();
        MCOP.readSeq(buffer, v, demarshaller);
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new SequenceReader());
  }

  <A> void addDemarshallerField(final int nr, final Demarshaller<A> demarshaller) {
    class DemarshallerReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
	A a = demarshaller.read(buffer);
        if (nr >= 0) t.setField(nr, a);
      }
    }
    fieldReaders.add(new DemarshallerReader());
  }
  
  void addStringSequenceField(final int nr) {
    class StringSequenceReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        ArrayList<String> v = new ArrayList<String>();
        MCOP.readSeq(buffer, v);
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new StringSequenceReader());
  }
  
  void addByteSequenceField(final int nr) {
    class ByteSequenceReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        ByteVector v = new ByteVector();
        MCOP.readSeq(buffer, v);
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new ByteSequenceReader());
  }
  
  void addLongSequenceField(final int nr) {
    class LongSequenceReader extends FieldReader {
      public void readField(Buffer buffer, T t) {
        IntVector v = new IntVector();
        MCOP.readSeq(buffer, v);
        if (nr >= 0) t.setField(nr, v);
      }
    }
    fieldReaders.add(new LongSequenceReader());
  }
  
  public T read(Buffer stream) throws MCOPException {
    try {
      // create instance
      T t = (T) typeInfo.structClass.newInstance(); // compiler warning ok
      
      // read and set fields
      for (int i = 0; i < fieldReaders.size(); i++) {
        //System.out.println(i+": "+fieldReaders.get(i));
        fieldReaders.get(i).readField(stream, t);
      }
      
      // return
      return t; // TODO
    } catch (InstantiationException e) {
      throw new MCOPException(e);
    } catch (IllegalAccessException e) {
      throw new MCOPException(e);
    }
  }
}
