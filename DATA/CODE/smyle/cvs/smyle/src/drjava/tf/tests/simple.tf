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

// Any function that is called test...() will be executed automatically
void testSomething() {
  error("Some error");
  System.out.println("System out in subtest");
}

// Output to System.out and System.err is captured
System.out.println("A message on System.out");
System.err.println("A message on System.err");

// Generate an error
error("Test error");

System.out.println("Another message on System.out");
System.err.println("Another message on System.err");

// Exceptions are captured as test events too
throw new Exception("An exception");