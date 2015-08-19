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

import org.artsProject.mcop.*;
import drjava.smyle.core.*;

public class Cascade<A,B,C> implements Function<A,C>, Marshallable {
  private final Function<A,B> f;
  private final Function<B,C> g;
  
  public Cascade(Function<A,B> f, Function<B,C> g) {
    this.f = f;
    this.g = g;
  }
  
  public C of(A a) {
    return g.of(f.of(a));
  }
  
  public Function<A,B> f() { 
    return f;
  }
  
  public Function<B,C> g() {
    return g;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof Cascade)) return false;
    Cascade c = (Cascade) o;
    return f.equals(c.f) && g.equals(c.g);
  }
  
  public int hashCode() {
    return f.hashCode()+g.hashCode();
  }
  
  public void marshal(Buffer b) {
    FunctionMarDemar.marshalCascade(b, f, g);
  }
  
  public String toString() {
    return "Cascade("+f+","+g+")";
  }
}
