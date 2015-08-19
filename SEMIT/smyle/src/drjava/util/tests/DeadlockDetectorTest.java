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

package drjava.util.tests;

import junit.framework.*;
import drjava.util.*;

public class DeadlockDetectorTest extends TestCase {
  public DeadlockDetectorTest(String name) { super(name); }

  Object object1 = new Object(), object2 = new Object();
  Thread thread1 = new Thread(), thread2 = new Thread();

  public void setUp() {
    DeadlockDetector.clear();
  }

  public void testNoDeadlock() {
    nested(thread1, object1, object2);
    nested(thread2, object1, object2);
  }

  public void testDeadlock() {
    try {
      nested(thread1, object1, object2);
      nested(thread2, object2, object1);
    } catch (Error e) {
      // ok
      return;
    }

    fail("Error expected");
  }

  public void testReleaseLock() {
    testNoDeadlock();
    testNoDeadlock();
  }

  void nested(Thread thread, Object a, Object b) {
    DeadlockDetector.gettingMonitor(thread, a);
    DeadlockDetector.gettingMonitor(thread, b);
    DeadlockDetector.releasingMonitor(thread, b);
    DeadlockDetector.releasingMonitor(thread, a);
  }

  public void testMethodsAreSynchronized() throws Exception {
    SynchronisationTester.assertPublicMethodsAreSynchronized(DeadlockDetector.class);
  }

  public void testBadOrder() {
    try {
      DeadlockDetector.gettingMonitor(thread1, object1);
      DeadlockDetector.gettingMonitor(thread1, object2);
      DeadlockDetector.releasingMonitor(thread1, object1);
    } catch (Error e) {
      // ok
      return;
    }

    fail("Error expected");
  }

  public void testReleaseBeforeGet() {
    try {
      DeadlockDetector.releasingMonitor(thread1, object1);
    } catch (Error e) {
      // ok
      return;
    }

    fail("Error expected");
  }
}
