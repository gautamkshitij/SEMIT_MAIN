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

package drjava.smyle.meta;

import java.util.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import java.lang.Object;
import drjava.smyle.*;

/** an extension of jMCOP's TypeInfo */
public class StructInfo<T extends Struct<T>> extends TypeInfo<T> implements Marshallable {
  public StructInfo[] fieldTypes;
  
  public StructInfo(TypeDef typeDef, Demarshaller<T> demarshaller, Class structClass,
    StructInfo[] fieldTypes) {
    super(typeDef, demarshaller, structClass);
    this.fieldTypes = fieldTypes;
  }

  public static final Demarshaller<StructInfo> DEMARSHALLER = new Demarshaller<StructInfo>() {
    public StructInfo read(Buffer buffer) {
      return new StructInfo(buffer);
    }
  };

  public int findTypeComponent(String name) {
    for (int i = 0; i < typeDef.contents.size(); i++) {
      if (typeDef.contents.get(i).name.equals(name))
        return i;
    }
    return -1;
  }

  public StructInfo(Buffer b) {
    super(new TypeDef(b), null, null);
  }

  public void marshal(Buffer b) {
    typeDef.marshal(b);
  }

  private static StructInfo findInCatalog(String name, ArrayList<StructInfo> catalog) {
    if (name.startsWith("*")) name = name.substring(1);
    for (int i = 0; i < catalog.size(); i++)
      if (catalog.get(i).typeDef.name.equals(name))
        return catalog.get(i);
    return null;
  }

  private static void fillFieldTypes(StructInfo info, ArrayList<StructInfo> catalog) {
    int n = info.typeDef.contents.size();
    info.fieldTypes = new StructInfo[n];
    for (int i = 0; i < n; i++) {
      TypeComponent c = info.typeDef.contents.get(i);
      StructInfo s = findInCatalog(c.type, catalog);
      //System.out.println("findInCatalog "+c.type+" => "+s);
      info.fieldTypes[i] = s;
      if (s != null && s.fieldTypes == null)
        fillFieldTypes(s, catalog);
    }
  }

  public static StructInfo readRecursive(Buffer b) {
    ArrayList<StructInfo> catalog = new ArrayList<StructInfo>();
    MCOP.readSeq(b, catalog, StructInfo.DEMARSHALLER);
    //System.out.println("Read catalog: "+catalog.size());

    fillFieldTypes(catalog.get(0), catalog);
    return catalog.get(0);
  }

  private void fillCatalog(ArrayList<StructInfo> catalog) {
    if (findInCatalog(typeDef.name, catalog) == null) {
      catalog.add(this);
      if (fieldTypes != null) for (int i = 0; i < fieldTypes.length; i++)
        if (fieldTypes[i] != null && findInCatalog(fieldTypes[i].typeDef.name, catalog) == null)
          fieldTypes[i].fillCatalog(catalog);
    }
  }

  public void writeRecursive(Buffer b) {
    ArrayList<StructInfo> catalog = new ArrayList<StructInfo>();
    //System.out.println("typeDef: "+typeDef+", fieldTypes: "+fieldTypes);
    fillCatalog(catalog);
    //System.out.println("Writing catalog: "+catalog.size());
    MCOP.writeSeq(b, catalog);
  }

  public boolean equals(Object o) {
    if (!(o instanceof StructInfo)) {
      //System.out.println("Wrong class: "+(o != null ? o.getClass() : null));
      return false;
    }
    if (o == this) return true;
    StructInfo s = (StructInfo) o;
    if ((typeDef != null) != (s.typeDef != null)) {
      //System.out.println("typeDef null/not-null");
      return false;
    }
    if (typeDef != null) {
      if (!typeDef.equals(s.typeDef)) {
        //System.out.println("Different typeDefs: "+typeDef+" != "+s.typeDef);
        return false;
      }
      for (int i = 0; i < fieldTypes.length; i++)
        if (fieldTypes[i] != null && s.fieldTypes[i] != null && !fieldTypes[i].equals(s.fieldTypes[i])) {
          //System.out.println("Field type "+i+": "+fieldTypes[i]+" != "+s.fieldTypes[i]);
          return false;
        }
    }
    //System.out.println("StructInfos are equal");
    return true;
  }
}
