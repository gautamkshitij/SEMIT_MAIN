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

package drjava.tf;

import java.util.*;
import java.io.*;
import drjava.util.Timer;
import drjava.util.RealTimer;

public class TestResult {
  private String name, description;
  private ArrayList<TestEvent> events = new ArrayList<TestEvent>();
  private ArrayList<String> errors = new ArrayList<String>();
  private ArrayList<TestResult> subtests = new ArrayList<TestResult>();
  private int totalNumberOfFailures = 0;
  Timer timer = new RealTimer();
  double runtime;
  
  PrintStream oldOut, oldErr;
  private ByteArrayOutputStream
    baosOut = new ByteArrayOutputStream(),
    baosErr = new ByteArrayOutputStream();
    
  TestResult(String name) {
    this.name = name;
  }
  
  public List<String> getErrors() { return errors; }
  
  public List<TestEvent> getEvents() {
    flush();
    return events;
  }
    
  public void error(String msg, String lineText) {
    flush();
    errors.add(msg);
    TestEvent e = new TestEvent(TestEvent.ERROR, msg);
    e.setLineText(lineText);
    events.add(e);
    ++totalNumberOfFailures;
  }
    
  private void flush() {
    String s = baosOut.toString();
    if (s.length() != 0) {
      baosOut.reset();
      events.add(new TestEvent(TestEvent.SYSTEMOUT, s));
    }
    
    s = baosErr.toString();
    if (s.length() != 0) {
      baosErr.reset();
      events.add(new TestEvent(TestEvent.SYSTEMERR, s));
    }
  }
  
  void redirectSystemStreams() {
    oldOut = System.out;
    oldErr = System.err;
    System.setOut(new PrintStream(baosOut));
    System.setErr(new PrintStream(baosErr));
  }
  
  void restoreSystemStreams() {
    System.setOut(oldOut);
    System.setErr(oldErr);
    oldOut = oldErr = null;
  }
  
  public String getTestName() {
    return name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public List<TestResult> getSubtests() {
    return subtests;
  }
  
  void addSubtest(TestResult test) {
    subtests.add(test);
    totalNumberOfFailures += test.totalNumberOfFailures;
  }
  
  void addException(Throwable e) {
    flush();
    events.add(new TestEvent(TestEvent.EXCEPTION, e.toString()));
    ++totalNumberOfFailures;
  }
  
  public int getTotalNumberOfFailures() {
    return totalNumberOfFailures;
  }
  
  public static boolean equals(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
  
  public void assertEquals(String desc, Object expected, Object actual, String lineText) {
    if (!equals(expected, actual))
      error(desc+(desc.length() != 0 ? " " : "")
        +"expected:<"+expected+"> but was:<"+actual+">", lineText);
  }
  
  public <A> void checkIterator(Iterator<A> it, A[] data, String lineText) {
    for (int i = 0; i < data.length; i++) {
      if (!it.hasNext())
        error("Iterator "+it+" ends at "+i+" of "+data.length, lineText);
      assertEquals("Element "+i+" of "+data.length, data[i], it.next(), lineText);
    }
    if (it.hasNext())
      error("Superfluous element "+it.next()+" after "+data.length, lineText);
  }
  
  public List<TestEvent> getAllEvents() {
    ArrayList<TestEvent> list = new ArrayList<TestEvent>();
    collectEvents(list);
    return list;
  }
  
  protected void collectEvents(List<TestEvent> list) {
    list.addAll( events);
    for (int i = 0; i < subtests.size(); i++)
      subtests.get(i).collectEvents(list);
  }
  
  /** time this test took to run in ms */
  public double getRuntime() {
    return runtime;
  }
  
  public void useTimer(Timer timer) {
    this.timer = timer;
  }
  
  public void description(String d) {
    description = d;
  }
}
