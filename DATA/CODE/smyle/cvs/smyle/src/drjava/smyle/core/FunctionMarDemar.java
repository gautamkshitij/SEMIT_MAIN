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
import drjava.smyle.*;
import drjava.smyle.meta.*;

public class FunctionMarDemar<T extends Struct<T>> implements MarDemar<Function> {
  private StructInfo<T> structInfo;
  
  private static final byte
    CASCADE     = 1,
    FIELD       = 2,
    TOLOWERCASE = 3;
  
  public FunctionMarDemar(StructInfo<T> structInfo) {
    this.structInfo = structInfo;
  }
  
  public void marshal(Buffer b, Function f) {
    ((Marshallable) f).marshal(b);
  }
  
  public Function read(Buffer b) {
    byte type = b.readByte();
    switch (type) {
      case CASCADE: {
        Function f = read(b);
        Function g = read(b);
        return new Cascade(f, g);
      }
      case FIELD: {
        return getField(b.readLong());
      }
      case TOLOWERCASE: {
        return ToLowerCase.INSTANCE;
      }
    }
    throw new InternalSmyleError("Unknown function type "+type);
  }
  
  public static void marshalCascade(Buffer b, Function f, Function g) {
    b.writeByte(CASCADE);
    ((Marshallable) f).marshal(b);
    ((Marshallable) g).marshal(b);
  }
  
  public static void marshalField(Buffer b, int nr) {
    b.writeByte(FIELD);
    b.writeLong(nr);
  }
  
  public static void marshalToLowerCase(Buffer b) {
    b.writeByte(TOLOWERCASE);
  }
  
  public Field getField(int nr) {
    if (structInfo.structClass == null)
      // untyped
      return new Field(nr);
    else try {
      // typed
      return (Field) structInfo.structClass.getField(
        "f_"+structInfo.typeDef.contents.get(nr).name).get(null);
    } catch (Exception e) {
      throw new InternalSmyleError(e);
    }
  }
}
