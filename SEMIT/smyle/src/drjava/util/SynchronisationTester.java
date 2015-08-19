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

import java.lang.reflect.*;
import junit.framework.*;

public class SynchronisationTester extends Assert {
  public interface MethodPredicate {
    public boolean evaluate(Method m);
  }
  
  public static void assertPackageAndPublicMethodsAreSynchronized(Class c) throws Exception {
    assertPackageAndPublicMethodsAreSynchronized(c, null);
  }
  
  public static void assertPackageAndPublicMethodsAreSynchronized(Class c, MethodPredicate pred) throws Exception {
    assertPublicMethodsAreSynchronized(c, pred);
    Method[] methods = c.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if ((m.getModifiers() &
        (Modifier.PRIVATE|Modifier.PROTECTED|Modifier.PUBLIC)) == 0
        && (pred == null || pred.evaluate(m)))
        checkMethod(m);
    }
  }
  
  public static void assertPublicMethodsAreSynchronized(Class c) throws Exception {
    assertPublicMethodsAreSynchronized(c, null);
  }
  
  public static void assertPublicMethodsAreSynchronized(Class c, MethodPredicate pred) throws Exception {
    Method[] methods = c.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.getDeclaringClass() != Object.class && (pred == null || pred.evaluate(m)))
        checkMethod(m);
    }
  }
  
  static void checkMethod(Method m) throws Exception {
    if ((m.getModifiers() & Modifier.SYNCHRONIZED) == 0) {
      String hint = null;
      if (m.getName().startsWith("access$"))
        hint = "This probably means that you are calling a private method from an inner class; "
          +"you might try making the method package private instead";
        
      fail(m+" is not synchronized"+(hint != null ? " ("+hint+")" : ""));
    }
  }
}
