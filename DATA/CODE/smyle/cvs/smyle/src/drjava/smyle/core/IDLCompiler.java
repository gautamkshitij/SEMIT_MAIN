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

package drjava.smyle.core;

import java.io.*;
import java.util.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import java.lang.Object;

public class IDLCompiler extends org.artsProject.mcop.IDLCompiler {
  ArrayList<StoreDef> stores = new ArrayList<StoreDef>();
  StoreDef store = new StoreDef();
  TypeComponent typeComp = new TypeComponent();

  public IDLCompiler() {
    loadSmyleExtensions();

    put("ModuleElement", new Object[][] {{ "Store" }});
    put("Store", new Object[][] {
      { "store", "IDENTIFIER", "{", "StoreElements_opt", "ClosingBracket", new Semantics() {
        public void evaluate(ParseTree tree) {
          store.name = (String) tree.phrase[1].attribute;
          stores.add(store);
          store = new StoreDef();
        }
      }}
    });

    putWithOptAndPlural("StoreElement", new Object[][] {
      { "table", "<", "IDENTIFIER", ">", "IDENTIFIER", ";", new Semantics() {
        public void evaluate(ParseTree tree) {
          store.tables.add(new TableDef((String) tree.phrase[2].attribute, 
                                        (String) tree.phrase[4].attribute));
        }
      }}
    });

    put("Field", new Object[][] {
      { "FieldModifiers_opt", "Type", "IDENTIFIER", ";", new Semantics() {
        public void evaluate(ParseTree tree) {
          typeComp.type = (String) tree.phrase[1].attribute;
          typeComp.name = (String) tree.phrase[2].attribute;
          type.contents.add(typeComp);
          typeComp = new TypeComponent();
        }
      }}
    });

    putWithOptAndPlural("FieldModifier", new Object[][] {
      { "autoincrement", new Semantics() {
        public void evaluate(ParseTree tree) {
          typeComp.hints.add("autoincrement");
        }
      }}
    });
  }
  
  protected boolean allowEnums() { return false; }
  protected boolean allowInterfaces() { return false; }

  public List<StoreDef> getStores() { return stores; }
}
