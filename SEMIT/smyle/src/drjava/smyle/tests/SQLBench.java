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

package drjava.smyle.tests;

import java.sql.*;
import drjava.util.*;

public class SQLBench extends Benchmark {
  static final int n = 100000;
  static final boolean lock = true;
  Connection con;
  
  public SQLBench() {
    setDescription("Inserting and deleting "+n+" rows");
  }
  
  void create() throws SQLException {
    con.createStatement().executeUpdate("drop table if exists people");
    con.createStatement().executeUpdate("create table people (name varchar(30), age bigint)");
  }
  
  void createIndex() throws SQLException {
    con.createStatement().executeUpdate("create index age on people (age)");
  }
  
  void optimizeTable() throws SQLException {
    con.createStatement().executeUpdate("optimize table people");
  }
  
  void lockTable() throws SQLException {
    if (lock) con.createStatement().executeUpdate("lock tables people write");
  }
  
  void unlockTable() throws SQLException {
    if (lock) con.createStatement().executeUpdate("unlock tables");
  }
  
  void add() throws SQLException {
    for (int i = 0; i < n; i++)
      con.createStatement().executeUpdate("insert people values ('"+i+"', "+i+")");
    done("insert "+n+" rows");
  }
  
  void removeFromTop() throws SQLException {
    for (int i = 0; i < n; i++)
      con.createStatement().executeUpdate("delete from people where age="+i);
    done("remove all records from top");
  }

  void addCombined() throws SQLException {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < n; i++)
      buf.append("insert people values ('"+i+"', "+i+");\n");
    con.createStatement().executeUpdate(buf.toString());
    done("insert "+n+" rows (combined)");
  }

  protected void action() {
    try {
      Class.forName("org.gjt.mm.mysql.Driver").newInstance();
      con = DriverManager.getConnection("jdbc:mysql://localhost/smyletest?user=smyletest&password=smyletest");
      
      create();
      
      lockTable();
      add();
      unlockTable();
      //addCombined(); //-->socket write error
      createIndex();
      //optimizeTable();
      lockTable();
      removeFromTop();
      unlockTable();
      //removeFromBottom();
      
      con.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }
  
  public static void main(String[] args) {
    SQLBench bench = new SQLBench();
    bench.runAndPrint();
    System.out.println("Records/s: "+(long) (n/(bench.totalTime()*0.001)));
  }
}

