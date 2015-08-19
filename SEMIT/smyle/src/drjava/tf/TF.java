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

import java.io.*;
import bsh.*;

public class TF {  
  private static void catchTargetError(TargetError e, TestResult result) {
    result.addException(e.getTarget());
  }
  
  public static TestResult runTest(String scriptFile) throws EvalError, IOException {
    TestResult result = new TestResult(scriptFile);
    
    result.redirectSystemStreams();
    
    try {
      Interpreter in = new Interpreter();     
      in.set("_result", result);
      
      // redirect methods - we should find a better way to do this
      in.eval(
        "void error(String msg, String invocationText) { _result.error(msg, invocationText); } \n"+
        "void error(String msg) { _result.error(msg, this.namespace.getInvocationText()); } \n"+
        "void assertTrue(boolean flag) { _result.assertEquals(\"\", true, flag, this.namespace.getInvocationText()); } \n"+
        "void assertFalse(boolean flag) { _result.assertEquals(\"\", false, flag, this.namespace.getInvocationText()); } \n"+
        "void assertEquals(expected, actual) { \n"+
        "  _result.assertEquals(\"\", expected, actual, this.namespace.getInvocationText()); \n"+
        "} \n"+
        "void checkIterator(a, b) { _result.checkIterator(a, b, this.namespace.getInvocationText()); } \n"+
        "void description(desc) { _result.description(desc); } \n"
      );
        
      try {
        in.source(scriptFile);
      } catch (TargetError e) {
        catchTargetError(e, result);
      }
      
      // find and run test methods
      boolean hasSetUp = (in.getNameSpace().getMethod("setUp", new Class[0]) != null);
      String[] methods = in.getNameSpace().getMethodNames();
      for (int i = methods.length-1; i >= 0; i--) {
        String name = methods[i];
        if (!(name.startsWith("test") || name.startsWith("bench"))) continue;
        TestResult subresult = new TestResult(name);
        subresult.redirectSystemStreams();
        try {
          in.set("_result", subresult);
          try {
            if (hasSetUp)
              in.eval("setUp();");
            double time = result.timer.getMilliseconds();
            in.eval(name+"();");
            subresult.runtime = result.timer.getMilliseconds()-time;
            /*if (name.startsWith("bench"))
              result.oldOut.println("Runtime="+subresult.runtime+", timer="+result.timer);*/
          } catch (TargetError e) {
            catchTargetError(e, result);
          }
          result.addSubtest(subresult);
        } finally {
          subresult.restoreSystemStreams();
        }
      }
    } finally {
      result.restoreSystemStreams();
    }
    
    return result;
  }
}
