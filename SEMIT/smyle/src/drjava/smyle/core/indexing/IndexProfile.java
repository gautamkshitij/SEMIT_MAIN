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

package drjava.smyle.core.indexing;

import org.artsProject.mcop.*;
import drjava.smyle.meta.*;

/** An index profile is a collection of fields that can be used to build an index or look for a
    suitable index for some query.
    It consists of two parts: a set of fields that may be reordered (permFields), and a set of
    fields that depend on the ordering (ordFields).
    permFields correspond to ...Equals(...) clauses, while ordFields correspond to orderBy...()
    clauses.
*/
public class IndexProfile {
  final Function[] permFields, ordFields;
  int hashCode;
  
  /*public static final MarDemar<IndexProfile> marDemar = new MarDemar<IndexProfile>() {
    public IndexProfile read(Buffer b) {
      Function[] permFields = readFunctionArray(b);
      Function[] ordFields = readFunctionArray(b);
      return new IndexProfile(permFields, ordFields);
    }
    
    public void marshal(Buffer b, IndexProfile profile) {
      writeFunctionArray(b, profile.permFields);
      writeFunctionArray(b, profile.ordFields);
    }
  };*/
  
  public IndexProfile(Function[] permFields, Function[] ordFields) {
    this.permFields = permFields;
    this.ordFields = ordFields;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof IndexProfile)) return false;
    IndexProfile i = (IndexProfile) o;
    return arrayEquals(permFields, i.permFields)
      && arrayEquals(ordFields, i.ordFields);
  }
  
  public int hashCode() {
    if (hashCode == 0) {
      for (int i = 0; i < permFields.length; i++)
        hashCode += permFields[i].hashCode()*i;
      for (int i = 0; i < ordFields.length; i++)
        hashCode += ordFields[i].hashCode()*(i+10);
    }
    return hashCode;
  }
  
  static boolean arrayEquals(Object[] a, Object[] b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++)
      if (!MCOP.equals(a[i], b[i])) return false;
    return true;
  }
  
  public String toString() {
    return "perm="+arrayToString(permFields)+" ord="+arrayToString(ordFields);
  }
  
  static String arrayToString(Object[] a) {
    StringBuffer buf = new StringBuffer("[");
    for (int i = 0; i < a.length; i++)
      buf.append(a[i]).append(i < a.length-1 ? " " : "");
    return buf.append(']').toString();
  }
  
  public boolean containsPermField(Function f) {
    for (int i = 0; i < permFields.length; i++)
      if (permFields[i].equals(f))
        return true;
    return false;
  }
}
