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

package drjava.smyle.browser;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.table.*;
import drjava.smyle.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import java.lang.Object;

class StoreView extends JPanel {
  JSplitPane split, hsplit;
  JPanel detailPanel = new JPanel(new BorderLayout());
  JPanel dataPanel   = new JPanel(new BorderLayout());
  JScrollPane detailScroll, dataScroll;
  JList tables;
  JTable data;
  Snapshot snapshot;
  String tableName;

  static Component multilineLabel(String text) {
    Box box = Box.createVerticalBox();
    while (text.length() != 0) {
      int i = text.indexOf('\n');
      if (i < 0) i = text.length();
      box.add(new JLabel(i == 0 ? " " : text.substring(0, i)));
      text = text.substring(Math.min(text.length(), i+1));
    }
    return box;
  }

  class DataModel extends AbstractTableModel {
    UntypedTable table;
    TypeDef schema;

    DataModel(UntypedTable table) {
      this.table = table;
      this.schema = table.getSchema();
    }

    public int getRowCount() {
      return table.size();
    }

    public int getColumnCount() {
      return schema.contents.size();
    }

    public Object getValueAt(int row, int col) {
      try {
        return table.untypedElementAt(row)[col];
      } catch (Exception e) {
        //e.printStackTrace();
        return e.toString();
      }
    }

    public String getColumnName(int i) {
      return schema.contents.get(i).name;
    }
  }

  void setDetail(Component detail) {
    detailPanel.removeAll();
    detailPanel.add(BorderLayout.CENTER, detail);
    detailScroll.validate();
  }

  void updateDetail() {
    StringBuffer buf = new StringBuffer();
    if (tableName != null) {
      UntypedTable table = snapshot.getUntypedTable(tableName);
      buf.append("Rows: "+table.size()+"\n\n");
      TypeDef schema = table.getSchema();
      if (schema != null) {
        buf.append("struct "+schema.name+" {\n");
        for (int i = 0; i < schema.contents.size(); i++) {
          TypeComponent c = schema.contents.get(i);
          buf.append("  ");
	  for (Iterator hints = c.hints.iterator(); hints.hasNext(); )
	    buf.append(hints.next()).append(' ');
	  buf.append(c.type+" "+c.name+";\n");
        }
        buf.append("}");
      }
    }
    setDetail(multilineLabel(buf.toString()));
  }

  void setData(JTable t) {
    data = t;
    dataScroll = new JScrollPane(data);
    dataPanel.removeAll();
    dataPanel.add(BorderLayout.CENTER, dataScroll);
    dataPanel.validate();
  }

  void updateData() {
    JTable data = new JTable();

    if (tableName != null) {
      UntypedTable table = snapshot.getUntypedTable(tableName);
      data.setModel(new DataModel(table));
    }

    setData(data);
  }

  void update() {
    String s = (String) tables.getSelectedValue();
    if (MCOP.equals(tableName, s)) return;
    tableName = s;
    updateDetail();
    updateData();
  }

  StoreView(Store store) {
    snapshot = store.snapshot();

    Vector<String> v = new Vector<String>();
    v.addAll(snapshot.getTableNames());
    tables = new JList((Vector) v);
    if (!v.isEmpty()) tables.setSelectedIndex(0);
    /*for (Iterator<String> i = snapshot.getTableNames().iterator(); i.hasNext(); ) {
      String name = i.next();
      UntypedTable table = snapshot.getUntypedTable(name);
      tables.add(name);
    }*/

    tables.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent evt) {
	update();
      }
    });
    
    JScrollPane tablesScroll = new JScrollPane(tables);
    tablesScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Tables"));
    detailScroll = new JScrollPane(detailPanel);
    detailScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Table Detail"));

    dataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Table Data"));

    setData(new JTable());

    update();

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    split.setLeftComponent(tablesScroll);
    split.setRightComponent(detailScroll);

    hsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    hsplit.setLeftComponent(split);
    hsplit.setRightComponent(dataPanel);
    hsplit.setDividerLocation(160);
    hsplit.validate();

    setLayout(new BorderLayout());
    add(hsplit);
  }
}

