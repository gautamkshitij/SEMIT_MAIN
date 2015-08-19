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

import java.util.*;
import drjava.smyle.core.*;
import drjava.smyle.meta.*;

public class QueryOptimizer {
  public static <T extends Struct<T>> IndexProfile createProfile(Filter<T> filter) {
    Function[] permFields = new Function[filter._numClauses()];
    for (int i = 0; i < filter._numClauses(); i++)
      permFields[i] = filter._getClause(i).getFunction();
      
    ArrayList<Function<T,Comparable>> order = filter._getOrder();
    Function[] ordFields = new Function[order.size()];
    order.toArray(ordFields);
      
    return new IndexProfile(permFields, ordFields);
  }
  
  public static int indexScore(IndexProfile wanted, Function[] available) {
    List list = Arrays.asList(available);
    return indexScore(wanted, list);
  }
  
  /** score = number of fields that match */
  public static int indexScore(IndexProfile wanted, List<Function> available) {
    // algorithm: advance through available fields as long as
    // there's a match within the profile (a perm field or an ord field at the right position)
    
    int i = 0;
    while (i < available.size()
      && wanted.containsPermField(available.get(i)))
        ++i;
        
    int j = i;
    while (j < available.size()
      && j-i < wanted.ordFields.length
      && wanted.ordFields[j-i].equals(available.get(j)))
        ++j;
      
    return j;
  }
}
