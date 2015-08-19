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

public class BenchmarkTest extends TestCase {
  public BenchmarkTest(String name) { super(name); }
  
  class MockTimer implements Timer {
    long time = 5; // Choose a different value than 0
    
    public double getMilliseconds() {
      return time;
    }
  }
  
  MockTimer timer;
  Benchmark bench;
  
  protected void setUp() {
    timer = new MockTimer();
    bench = new Benchmark(timer) {
      int rep;
      
      public void action() {
        spendTime(100+(rep++));
      }
    };
  }
  
  void spendTime(double t) {
    timer.time += t;
  }
  
  public void testSimpleBench() {
    bench.run();
    assertEquals(100f, bench.totalTime(), 0f);
  }
  
  public void testAverage() {
    bench.setRepetitions(11);
    bench.run();
    assertEquals(105f, bench.totalTime(), 0f);
  }
  
  public void testToString() {
    bench.setDescription("bla");
    bench.run();
    assertEquals("bla: 100 ms", bench.toString());
  }
  
  public void testDone() {
    bench = new Benchmark(timer) {
      public void action() {
        spendTime(33);
        done("part 1");
        spendTime(55);
        done("part 2");
      }
    };
    bench.run();
    assertEquals("total: 88 ms\n"
      +"-part 1: 33 ms\n"
      +"-part 2: 55 ms", bench.toString());
  }
}
