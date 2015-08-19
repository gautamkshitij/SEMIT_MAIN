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

package drjava.util;

import java.util.*;
import junit.framework.*;

public class TestUtil extends Assert {
  public static <A> void checkIterator(Iterator<A> it, A[] data) {
    for (int i = 0; i < data.length; i++) {
      assertTrue("Iterator "+it+" ends at "+i+" of "+data.length, it.hasNext());
      assertEquals("Element "+i+" of "+data.length, data[i], it.next());
    }
    if (it.hasNext())
      fail("Superfluous element "+it.next()+" after "+data.length);
  }
  
  public static <A> void checkList(List<A> l, A[] data) {
    checkIterator(l.iterator(), data);
  }
}
