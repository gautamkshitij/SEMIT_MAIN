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

public class PrefixKeySet implements KeySet<UniversalKey> {
  UniversalKey prefix;
  
  public PrefixKeySet(UniversalKey prefix) {
    this.prefix = prefix;
  }
  
  public boolean contains(UniversalKey k) {
    if (k.data.length < prefix.data.length) return false;
    for (int i = 0; i < prefix.data.length; i++)
      if (prefix.data[i] != k.data[i])
        return false;
    return true;
  }

  public boolean overlapsWithRange(UniversalKey min, UniversalKey max) {
    if (max != null && max.compareTo(prefix) < 0)
      return false; // end of range is lower than prefix
    if (min != null && min.compareTo(prefix) > 0)
      if (!contains(min))
        return false; // beginning of range is higher than prefix
    return true;
  }
}
