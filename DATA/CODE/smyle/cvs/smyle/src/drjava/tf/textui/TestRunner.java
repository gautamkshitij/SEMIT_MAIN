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

package drjava.tf.textui;

import java.util.*;
import drjava.tf.*;

public class TestRunner {
  static String stripLineSeparator(String s) {
    String linesep = System.getProperty("line.separator");
    if (s.endsWith(linesep))
      return s.substring(0, s.length()-linesep.length());
    else
      return s;
  }
  
  static void printTestResult(String indent, TestResult result) {
    // print test name
    System.out.println(indent+" == "+result.getTestName()+" == "
      +(result.getTotalNumberOfFailures() != 0 ? result.getTotalNumberOfFailures()+" failure(s)" : "")
      +(result.getTestName().startsWith("bench") ? " "+result.getRuntime()+" ms" : ""));
    if (result.getDescription() != null)
      System.out.println(indent+"  = "+result.getDescription()+" =");
    
    // print events
    List<TestEvent> events = result.getEvents();
    for (int i = 0; i < events.size(); i++) {
      TestEvent e = events.get(i);
      System.out.println(indent
        +(e.isFailure() ? " !" : "  ")
        +stripLineSeparator(e.getText()));
      if (e.getLineText() != null)
        System.out.println(indent+"    "+e.getLineText());
    }
    
    // print subtests
    indent += "  ";
    List<TestResult> subtests = result.getSubtests();
    for (int i = 0; i < subtests.size(); i++)
      printTestResult(indent, subtests.get(i));
      
    System.out.println();    
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage: drjava.tf.textui.TestRunner <scriptfiles>");
      System.exit(2);
    }
    
    int failures = 0;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < args.length; i++) {
      TestResult result = TF.runTest(args[i]);
      printTestResult("", result);
      failures += result.getTotalNumberOfFailures();
    }
    
    long endTime = System.currentTimeMillis();   
    System.out.println("Tests took "+((endTime-startTime)/1000.0f)+" sec");
    System.out.println();
    
    if (failures != 0)
      System.exit(1);
    else
      System.exit(0);
  }
}
