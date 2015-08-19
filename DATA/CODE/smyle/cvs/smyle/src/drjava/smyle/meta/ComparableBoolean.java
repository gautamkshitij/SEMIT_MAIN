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

/** an enhanced clone of java.lang.Boolean that implements java.lang.Comparable */

public final class ComparableBoolean implements Comparable {
  private final boolean value;
  
  public static final ComparableBoolean  TRUE = new ComparableBoolean( true);
  public static final ComparableBoolean FALSE = new ComparableBoolean(false);
  
  private ComparableBoolean(boolean value) {
    this.value = value;
  }
  
  public int compareTo(Object o) {
    return intValue()-((ComparableBoolean) o).intValue();
  }
  
  int intValue() {
    return value ? 1 : 0;
  }
  
  public boolean booleanValue() {
    return value;
  }
  
  public String toString() {
    return value ? "true" : "false";
  }
}
