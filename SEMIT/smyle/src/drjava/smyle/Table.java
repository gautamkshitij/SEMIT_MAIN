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

package drjava.smyle;

import java.util.*;
import org.artsProject.mcop.Type;
import org.artsProject.mcop.core.TypeDef;
import org.artsProject.util.*;
import drjava.smyle.meta.*;

/** A data table in a Smyle store.
    Table is an extension of the java.util.List interface, so you can use a table just like you
    use a standard Java Vector. Additionally, table defines methods to perform convenient
    and efficient queries.
    <p>
    Every row in the table is of type "T", where T must be a class generated from an IDL
    file by the CodeGenerator.
    */
public interface Table<T extends Struct<T>> extends java.util.List<T> {
  // methods inherited from java.util.List (repeated for completeness)
  
  public int size();
  public boolean isEmpty();
  public T get(int nr);
  public Iterator<T> iterator();
  public int indexOf(T t);
  public boolean add(T t);
  public boolean addAll(Collection<T> c);
  public boolean remove(T t);
  public T remove(int index);
  public T set(int index, T t);
  public void clear();
  
  // accessors
  
  public T first();
  
  // filter based accessors
  
  public boolean     contains(Filter<T> filter);
  public int         count   (Filter<T> filter);
  public Iterator<T> iterator(Filter<T> filter);
  
  /** if there are any records that satisfy the filter, get returns one of
      them; otherwise it returns null */
  public T           get(Filter<T> filter);
  public int         indexOf(Filter<T> filter);
  public List<T>     subList(Filter<T> filter);
  
  /** if there is a record that satisfies the filter, returns it;
      otherwise creates an instance of T that matches the filter and
      has default values in all fields not tested by the filter */
  public T           getOrCreate(Filter<T> filter);
  
  /** if there is a record that satisfies the filter, replace it with t;
      otherwise add t to the table */
  public void        put(Filter<T> filter, T t);
  
  // function based accessors
  
  public IntVector getUniqueValues(Function<T,Integer> function);
  public <A> Map<A,Integer> getUniqueValueCounts(Function<T,A> function);
  public <A> Map<A,Integer> getUniqueValueCounts(Function<T,A> function, Filter<T> filter);
  
  // filter based mutators
  
  public void removeAll(Filter<T> filter);
  
  // schema handling
  
  public TypeDef getSchema();
  
  // auto increment
  
  public void setAutoIncrementField(Field<T,Integer> field);
}
