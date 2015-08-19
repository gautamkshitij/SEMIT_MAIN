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

public class TestEvent {
  // Event types
  public static final int
    SYSTEMOUT = 0,
    SYSTEMERR = 1,
    ERROR     = 2,
    EXCEPTION = 3;
    
  private int type;
  private String text;
  private int lineNumber;
  private String lineText;
    
  public TestEvent(int type, String text) {
    this.type = type;
    this.text = text;
  }
  
  public int getType() { return type; }
  public String getText() { return text; }
  
  public boolean equals(Object o) {
    if (!(o instanceof TestEvent)) return false;
    TestEvent e = (TestEvent) o;
    return e.type == type && e.text.equals(text);
  }
  
  public String toString() {
    return text;
  }
  
  public boolean isFailure() {
    return type == ERROR || type == EXCEPTION;
  }
  
  void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }
  
  void setLineText(String lineText) {
    this.lineText = lineText;
  }
  
  public int getLineNumber() {
    return lineNumber;
  }
  
  public String getLineText() {
    return lineText;
  }
}
