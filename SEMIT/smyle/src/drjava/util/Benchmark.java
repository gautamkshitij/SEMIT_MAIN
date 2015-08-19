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

import java.util.*;

public abstract class Benchmark {
  private Timer timer;
  private double total, partStart;
  private int repetitions = 1;
  private String description = "total";
  private Vector<String> partNames = new Vector<String>();
  private Vector<Double> partTimes = new Vector<Double>();
  
  protected abstract void action();
  
  public Benchmark(Timer timer) {
    this.timer = timer;
  }
  
  public Benchmark() {
    this.timer = new RealTimer();
  }
  
  public double totalTime() {
    return total;
  }
  
  public void run() {
    double startTime = timer.getMilliseconds();
    partStart = startTime;
    for (int i = 0; i < repetitions; i++)
      action();
    double endTime = timer.getMilliseconds();
    total = (endTime-startTime)/repetitions;
  }
  
  public void setRepetitions(int n) {
    repetitions = n;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  String formatTime(double t) {
    String time = String.valueOf(t);
    if (time.endsWith(".0")) time = time.substring(0, time.length()-2);
    return time+" ms";
  }
  
  public String toString() {
    StringBuffer buf = new StringBuffer(description+": "+formatTime(totalTime()));
    for (int i = 0; i < partNames.size(); i++)
      buf.append("\n-").append(partNames.get(i))
        .append(": ").append(formatTime(partTimes.get(i).doubleValue()));
    return buf.toString();
  }
  
  public void runAndPrint() {
    run();
    System.out.println(this);
  }
  
  public void done(String partName) {
    partNames.add(partName);
    double time = timer.getMilliseconds();
    partTimes.add(new Double(time-partStart));
    partStart = time;
  }
}
