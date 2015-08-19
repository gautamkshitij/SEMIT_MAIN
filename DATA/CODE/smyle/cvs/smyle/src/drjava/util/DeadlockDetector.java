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

public class DeadlockDetector {
  static /*Weak*/HashMap<Thread,ArrayList<Object>> locks = new /*Weak*/HashMap<Thread,ArrayList<Object>>();
  static /*Weak*/HashMap<Object,HashSet<Object>> order = new /*Weak*/HashMap<Object,HashSet<Object>>();

  public static synchronized void clear() {
    locks.clear();
  }

  static void addToOrder(Object a, Object b) {
    if (a == b) return;
    HashSet<Object> s = order.get(a);
    if (s == null)
      order.put(a, s = new HashSet<Object>());
    s.add(b);
  }

  public static synchronized void gettingMonitor(Thread thread, Object object) {
    ArrayList<Object> l = locks.get(thread);
    if (l == null)
      locks.put(thread, l = new ArrayList<Object>());

    for (int i = 0; i < l.size(); i++)
      addToOrder(l.get(i), object);

    l.add(object);

    detectCycle(object);
  }

  static void detectCycle(Object o) {
    detectCycle(new HashSet<Object>(), o);
  }

  static void detectCycle(HashSet<Object> s, Object o) {
    if (s.contains(o))
      throw new Error("Deadlock detected!");
    s.add(o);
    HashSet<Object> hs = order.get(o); 
    if (hs != null)
      for (Iterator<Object> i = hs.iterator(); i.hasNext(); )
        detectCycle(s, i.next());
  }

  public static synchronized void releasingMonitor(Thread thread, Object object) {
    ArrayList<Object> l = locks.get(thread);
    if (l == null)
      throw new Error("release before get");
    if (l.get(l.size()-1) != object)
      throw new Error("Bad get/release order");
    l.remove(l.size()-1);
    if (l.isEmpty())
      locks.remove(thread);
  }
}
