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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import drjava.smyle.*;

/*
TODO: check for in-use.flag
*/

public class Main extends JFrame {
  static final String
    aOpen  = "Open store...",
    aClose = "Close store",
    aExit  = "Exit";

  String storeDir;
  Store store;
  StoreView view;
  JMenuItem iClose;

  void openStore(String dir) {
    try {
      if (store != null) store.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    store = null;

    storeDir = dir;
    if (storeDir != null) {
      if (storeDir.endsWith("/")) storeDir = storeDir.substring(0, storeDir.length()-1);

      if (!Smyle.storeExists(storeDir)) {
        JOptionPane.showMessageDialog(this, "No Smyle store found in "+storeDir, "Error", JOptionPane.OK_OPTION);
      } else try {
	if (Smyle.storeInUse(storeDir))
	  if (JOptionPane.showConfirmDialog(this, 
	    "The store in "+storeDir+" seems to be in use by another process.\n"
	    +"This can cause the browser to hang. Open anyway?",
	    "Warning", JOptionPane.YES_NO_OPTION) 
	    != JOptionPane.YES_OPTION) {
	    update();
	    return;
	  }
        store = Smyle.openStoreReadOnly(storeDir);
      } catch (Exception e) {
        store = null;
        JOptionPane.showMessageDialog(this, e.getMessage());
      }
    }

    update();
  }

  static ButtonModel mnemonic(char c) {
    DefaultButtonModel model = new DefaultButtonModel();
    model.setMnemonic((int) c);
    return model;
  }

  void exit() {
    if (store != null)
      store.close();
    System.exit(0);
  }

  Main() { 
    updateTitle();
    setSize(500, 400);
    getContentPane().setLayout(new BorderLayout());

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	exit();
      }
    });

    JMenu mFile = new JMenu("File");
    mFile.setModel(mnemonic('f'));

    JMenuItem item = mFile.add(aOpen);
    item.setModel(mnemonic('o'));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	FileDialog dialog = new FileDialog(Main.this, "Open store");
	dialog.setFilenameFilter(new FilenameFilter() {
	  public boolean accept(File dir, String name) {
	    name = name.toLowerCase();
	    return name.startsWith("m") && name.endsWith(".smy");
	  }
	});
	dialog.show();

	if (dialog.getDirectory() != null) {
	  openStore(dialog.getDirectory());
        }
      }
    });

    item = iClose = mFile.add(aClose);
    item.setModel(mnemonic('c'));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	openStore(null);
      }
    });

    mFile.addSeparator();

    item = mFile.add(aExit);
    item.setModel(mnemonic('e'));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	exit();
      }
    });

    JMenuBar menubar = new JMenuBar();
    menubar.add(mFile);
    setJMenuBar(menubar);
  }

  void updateTitle() {
    setTitle("Smyle Browser "+Smyle.getVersion()+(store == null ? "" : " ["+storeDir+"]"));
  }

  void update() {
    updateTitle();
    if (view != null) getContentPane().remove(view);
    if (store == null) {
      view = null;
      getContentPane().repaint();
    } else {
      view = new StoreView(store);
      getContentPane().add(BorderLayout.CENTER, view);
    }
    getContentPane().validate();
    iClose.setEnabled(store != null);
  }

  public static void main(String[] args) throws Exception {
    Main main = new Main();
    main.show();
    if (args.length != 0)
      main.openStore(args[0]);
    Object o = new Object();
    synchronized(o) { o.wait(); }
  }
}
