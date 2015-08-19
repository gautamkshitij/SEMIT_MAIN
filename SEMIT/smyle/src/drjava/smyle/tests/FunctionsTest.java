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

package drjava.smyle.tests;

import junit.framework.*;
import drjava.smyle.meta.*;

public class FunctionsTest extends TestCase {
  public FunctionsTest(String name) { super(name); }
  
  public void testEqualsAndHashCodes() {
    Cascade c1 = new Cascade(ToLowerCase.INSTANCE, ToLowerCase.INSTANCE);
    Cascade c2 = new Cascade(ToLowerCase.INSTANCE, ToLowerCase.INSTANCE);
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());

    Field f1 = new Field(0), f2 = new Field(0);
    assertEquals(f1, f2);
    assertEquals(f1.hashCode(), f2.hashCode());
  }
}
