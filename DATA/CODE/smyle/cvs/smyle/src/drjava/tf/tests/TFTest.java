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

package drjava.tf.tests;

import java.util.*;
import drjava.tf.*;
import drjava.util.TestUtil;
import junit.framework.TestCase;

/** a JUnit test that tests the Test Framework */
public class TFTest extends TestCase {
  public TFTest(String name) { super(name); }

  /** tests all features at once */    
  public void testComplete() throws Exception {
    String linesep = System.getProperty("line.separator");
    
    TestResult result = TF.runTest("src/drjava/tf/tests/simple.tf");
    
    // 2 failures in main test + 1 failure in subtest
    assertEquals(3, result.getTotalNumberOfFailures());
    
    // Check main test
    assertEquals("src/drjava/tf/tests/simple.tf", result.getTestName());
    List<TestEvent> events = result.getEvents();
    TestUtil.checkList(events, new TestEvent[] { 
      new TestEvent(TestEvent.SYSTEMOUT, "A message on System.out"+linesep),
      new TestEvent(TestEvent.SYSTEMERR, "A message on System.err"+linesep),
      new TestEvent(TestEvent.ERROR, "Test error"), 
      new TestEvent(TestEvent.SYSTEMOUT, "Another message on System.out"+linesep),
      new TestEvent(TestEvent.SYSTEMERR, "Another message on System.err"+linesep),
      new TestEvent(TestEvent.EXCEPTION, "java.lang.Exception: An exception"),
    });
    
    assertEquals(false, events.get(0).isFailure());
    assertEquals(false, events.get(1).isFailure());
    assertEquals(true,  events.get(2).isFailure());
    assertEquals(true,  events.get(5).isFailure());
    
    //XXXassertEquals(12, events.get(2).getLineNumber());
    
    // Check subtest list
    List<TestResult> subtests = result.getSubtests();
    assertEquals(1, subtests.size());
    
    // Check subtest
    result = subtests.get(0);
    assertEquals("testSomething", result.getTestName());
    TestUtil.checkList(result.getEvents(), new TestEvent[] { 
      new TestEvent(TestEvent.ERROR, "Some error"),
      new TestEvent(TestEvent.SYSTEMOUT, "System out in subtest"+linesep),
    });
  }
  
  public void testAsserts() throws Exception {
    TestResult result = TF.runTest("src/drjava/tf/tests/asserts.tf");
    TestUtil.checkList(result.getEvents(), new TestEvent[] { 
      new TestEvent(TestEvent.ERROR, "expected:<1> but was:<2>"),
    });
  }
  
  public void testSetUp() throws Exception {
    TestResult result = TF.runTest("src/drjava/tf/tests/setup.tf");
    TestUtil.checkList(result.getAllEvents(), new TestEvent[] { 
      new TestEvent(TestEvent.ERROR, "setUp"),
      new TestEvent(TestEvent.ERROR, "test1"),
      new TestEvent(TestEvent.ERROR, "setUp"),
      new TestEvent(TestEvent.ERROR, "test2"),
    });
  }
  
  public void testBench() throws Exception {
    TestResult result = TF.runTest("src/drjava/tf/tests/bench.tf");
    
    List<TestResult> subtests = result.getSubtests();
    assertEquals(1, subtests.size());
    
    result = subtests.get(0);
    assertEquals("benchSomething", result.getTestName());
    assertEquals("Waste 100 ms", result.getDescription());
    assertEquals(100.0, result.getRuntime(), 1.0);
  }
}
