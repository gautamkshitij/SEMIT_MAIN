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

package drjava.smyle;

import java.io.*;
import java.util.*;
import org.artsProject.mcop.*;
import org.artsProject.mcop.core.*;
import drjava.util.*;
import drjava.smyle.core.IDLCompiler;
import drjava.smyle.core.*;

/** generates Java sources from an IDL file */
public class CodeGenerator extends org.artsProject.mcop.CodeGenerator {
  protected final String
    cFilter            = "drjava.smyle.meta.Filter",
    cFunction          = "drjava.smyle.meta.Function",
    cField             = "drjava.smyle.meta.Field",
    cCascade           = "drjava.smyle.meta.Cascade",
    cStruct            = "drjava.smyle.meta.Struct",
    cStructInfo        = "drjava.smyle.meta.StructInfo",
    cComparableBoolean = "drjava.smyle.meta.ComparableBoolean",
    cArrayList         = "java.util.ArrayList",
    cToLowerCase       = "drjava.smyle.meta.ToLowerCase",
    cStore             = "drjava.smyle.Store",
    cTable             = "drjava.smyle.Table",
    cSnapshot          = "drjava.smyle.Snapshot",
    cDisk              = "drjava.smyle.core.Disk",
    cDiskStore         = "drjava.smyle.core.DiskStore",
    cStoreRegistry     = "drjava.smyle.core.StoreRegistry";
    
  protected final String
    throwBadFieldException = "throw new IllegalArgumentException(\"Bad field id: \"+_nr);";

  String destDir;
  ArrayList<String> sourceFiles = new ArrayList<String>();
    
  protected String getTypeSuperclass(TypeDef type) {
    return cStruct+"<"+stripPackage(type.name)+">";
  }
  
  static String capitalize(String s) {
    return Character.toUpperCase(s.charAt(0))+s.substring(1);
  }
    
  public void generate() throws IOException {
    super.generate();
    generateFilters();
    generateConstructors();
  }
  
  String box(String jtype) {
    if (jtype.equals("int"))
      return "Integer";
    else if (jtype.equals("boolean"))
      return cComparableBoolean;
    else if (jtype.equals("long"))
      return "Long";
    else
      return jtype;
  }
  
  String boxExp(String exp, String jtype) {
    if (jtype.equals("int"))
      return "new Integer("+exp+")";
    else if (jtype.equals("boolean"))
      return exp+" ? "+cComparableBoolean+".TRUE : "+cComparableBoolean+".FALSE";
    else if (jtype.equals("long"))
      return "new Long("+exp+")";
    else
      return exp;
  }
  
  String unboxExp(String exp, String jtype) {
    if (jtype.equals("int") || jtype.equals("boolean") || jtype.equals("long"))
      return "("+exp+")."+jtype+"Value()";
    else
      return exp;
  }
  
  protected PrintWriter getWriter(String className) throws IOException {
    String file = packageDir+className+".java";
    sourceFiles.add(new File(destDir, file).getPath());
    return new PrintWriter(saver.getWriter(file));
  }

  private void generateFilters() throws IOException {
    for (Iterator<TypeDef> types = mod.types.iterator(); types.hasNext(); ) {
      TypeDef type = types.next();
      String typeName = stripPackage(type.name);
      String qName = typeName+"_filter";
      
      PrintWriter w = getWriter(qName);
      w.println(fileHeader);
      w.println("public class "+qName+" extends "+cFilter+"<"+typeName+"> {");
        
      // clause methods
      
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        String jtype = type2java(c.type);
        
        w.println("  ");
        w.println("  public "+qName+" "+c.name+"Equals(final "+jtype+" "+c.name+") {");
        w.println("    clauses.add(new "+cFilter+".FunctionEquals<"+box(jtype)+">("
          +typeName+".f_"+c.name+", "+boxExp(c.name, jtype)+"));");
        w.println("    return this;");
        w.println("  }");
        
        if (c.type.equals("string")) {
          w.println("  ");
          w.println("  public "+qName+" "+c.name+"EqualsIgnoreCase(final String "+c.name+") {");
          w.println("    clauses.add(new "+cFilter+".FunctionEquals<String>(new "
            +cCascade+"<"+typeName+",String,String>("
            +typeName+".f_"+c.name+", "+cToLowerCase+".INSTANCE), "+c.name+".toLowerCase()));");
          w.println("    return this;");
          w.println("  }");
        }
      }
      
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        String jtype = type2java(c.type);
        
        // nested fields if struct field
        TypeDef type2 = findType(c.type);
        if (type2 != null) {
          String typeName2 = stripPackage(type2.name);
          for (Iterator<TypeComponent> e2 = type2.contents.iterator(); e2.hasNext();) {
            TypeComponent c2 = e2.next();
            String jtype2 = type2java(c2.type);
            String ftype2 = getFieldType(typeName, c2.type);
            
            w.println("  ");
            w.println("  public "+qName+" "+c.name+"_"+c2.name+"Equals(final "+jtype2+" "+c2.name+") {");
            /*if (isPrimitive(c2.type)) {
              String boxed = boxedJType(c2.type);
              w.println("    clauses.add(new "+cFilter+".FunctionEquals<"+boxed+">("
                +cFunctionUtil+".asFunction("+typeName+".f_"+c.name+"_"+c2.name
                +"), new "+boxed+"("+c2.name+")));");
            } else */
              w.println("    clauses.add(new "+cFilter+".FunctionEquals<"+box(jtype2)+">("
                +typeName+".f_"+c.name+"_"+c2.name+", "+boxExp(c2.name, jtype2)+"));");
            w.println("    return this;");
            w.println("  }");
          }
        }
      }
      
      // orderBy methods
            
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        if (c.type.startsWith("*") || isType(c.type)) continue;
        String jtype = type2java(c.type);
        
        if (jtype.equals("int") || jtype.equals("long") || jtype.equals("boolean")
          || c.type.equals("string")) {
          w.println("  ");
          w.println("  public "+qName+" orderBy"+capitalize(c.name)+"() {");
          w.println("    order.add(("+cFunction+") "+typeName+".f_"+c.name+");");
          w.println("    return this;");
          w.println("  }");
        }
      }
      
      // reverse method
      
      w.println("  public "+qName+" reverse() {");
      w.println("    reverse = !reverse;");
      w.println("    return this;");
      w.println("  }");
      
      // createElement method
      
      w.println("  protected "+typeName+" createElement() {");
      w.println("    return new "+typeName+"();");
      w.println("  }");
      
      // close class
      
      w.println("}");
      w.close();
    }
  }
  
  String getFieldType(String a, String b) {
    return mangle(cField, b, a);
  }
  
  String getFunctionType(String a, String b) {
    return mangle(cFunction, b, a);
  }
  
  String getCascadeType(String a, String b, String c) {
    return mangle(cCascade, c, type2java(a)+","+type2java(b));
  }
  
  String mangle(String className, String type, String extraParams) {
    String jtype = type2java(type);
    return className+"<"+extraParams+","+box(jtype)+">";
  }
  
  private void generateConstructors() throws IOException {
    for (Iterator<TypeDef> types = mod.types.iterator(); types.hasNext(); ) {
      TypeDef type = types.next();
      String typeName = stripPackage(type.name);
      String cName = typeName+"_constructor";
      
      PrintWriter w = getWriter(cName);
      w.println(fileHeader);
      w.println("public class "+cName+"<T extends "+cStruct+"<T>> implements "+cFunction+"<T,"+typeName+"> {");

      // fields
        
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        w.println("  private final "+getFunctionType("T", c.type)+" "+c.name+";");
      }
      
      // constructor
      
      w.println();
      w.println("  public "+cName+"(");
      boolean first = true;
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        
        if (first) first = false; else w.println(",");
        w.print("    "+getFunctionType("T", c.type)+" "+c.name);
      }
      w.println();
      w.println("  ) {");
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        w.println("    this."+c.name+" = "+c.name+";");
      }
      w.println("  }");
      
      // get method
      
      w.println();
      w.println("  public "+typeName+" of(T _t) {");
      w.print  ("    return new "+typeName+"(");
      first = true;
      for (Iterator<TypeComponent> e = type.contents.iterator(); e.hasNext();) {
        TypeComponent c = e.next();
        
        if (first) first = false; else w.print(", ");
        w.print(unboxExp(c.name+".of(_t)", type2java(c.type)));
      }
      w.println(");");
      w.println("  }");
      
      // close class
      
      w.println("}");
      w.close();
    }
  }
  
  static String removeParams(String jtype) {
    int idx = jtype.indexOf('<');
    return idx < 0 ? jtype : jtype.substring(0, idx);
  }
  
  protected void printTypeContents(PrintWriter w, TypeDef type) throws IOException {
    super.printTypeContents(w, type);
    String typeName = stripPackage(type.name);
    
    // f_... meta fields
    
    w.println("  ");
    for (int i = 0; i < type.contents.size(); i++) {
      TypeComponent c = type.contents.get(i);
      String ftype = getFieldType(typeName, c.type);
      w.println("  public static final "+ftype+" f_"+c.name+" = new "+ftype+"("+i+");");
    
      // nested fields if struct field
      TypeDef type2 = findType(c.type);
      if (type2 != null) {
        String typeName2 = stripPackage(type2.name);
        for (Iterator<TypeComponent> e2 = type2.contents.iterator(); e2.hasNext();) {
          TypeComponent c2 = e2.next();
          String jtype2 = type2java(c2.type);
          String ftype2 = getFieldType(typeName, c2.type);
          
          w.println("  public static final "+getFunctionType(typeName, c2.type)+" f_"+c.name+"_"+c2.name
            +" = new "+getCascadeType(typeName, typeName2, c2.type)
            +"(f_"+c.name+","+typeName2+".f_"+c2.name+");");
        }
      }
    }
    
    // get...(), set...() methods

    w.println("  ");
    w.println("  public Object getField(int _nr) {");
    for (int i = 0; i < type.contents.size(); i++) {
      TypeComponent c = type.contents.get(i);
      String jtype = type2java(c.type);
      
      w.println("    if (_nr == "+i+") return "+boxExp(c.name, jtype)+";");
    }
    w.println("    "+throwBadFieldException);
    w.println("  }");
    
    w.println("  ");
    w.println("  public void setField(int _nr, Object _value) {");
    for (int i = 0; i < type.contents.size(); i++) {
      TypeComponent c = type.contents.get(i);
      String jtype = type2java(c.type);
      
      if (c.type.startsWith("*"))
        w.println("    if (_nr == "+i+") { "+c.name+".clear(); "+c.name+".addAll(("+removeParams(jtype)+") _value); return; }");
      else
        w.println("    if (_nr == "+i+") { "+c.name+" = "+unboxExp("("+box(jtype)+") _value", jtype)+"; return; }");
    }
    w.println("    "+throwBadFieldException);
    w.println("  }");
  }
  
  protected void printTypeInfo(PrintWriter w, TypeDef type, Buffer marshalledType) {
    String typeName = stripPackage(type.name);
    w.println("  private static final "+cStructInfo+"<"+typeName+"> TYPE = new "+cStructInfo+"<"+typeName+">(");
    w.println("    new "+cTypeDef+"(new "+cBuffer+"(\""+marshalledType+"\")),");
    w.println("    DEMARSHALLER,");
    w.println("    "+typeName+".class,");
    w.println("    null);");
    w.println();
    w.println("  static {");
    w.println("    TYPE.fieldTypes = new "+cStructInfo+"[] { ");
    for (int i = 0; i < type.contents.size(); i++) {
      TypeComponent c = type.contents.get(i);
      TypeDef type2 = findType(c.type);
      if (type2 == null && c.type.startsWith("*"))
        type2 = findType(c.type.substring(1));
      if (type2 != null)
        w.println("      "+stripPackage(type2.name)+".getTypeInfo(),");
      else
        w.println("      null,");
    }
    w.println("    };");
    w.println("  }");
    w.println();
    w.println("  public static "+cStructInfo+"<"+typeName+"> getTypeInfo() { return TYPE; }");
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length < 3 || args.length > 4) {
      System.err.println("usage: "+CodeGenerator.class.getName()+" <idlFile> <packageName> <destSrcDir>");
      System.err.println("       (generate GJ sources in destSrcDir)");
      System.err.println("   or: "+CodeGenerator.class.getName()+" <idlFile> <packageName> <destSrcDir> <destClassDir>");
      System.err.println("       (generate GJ sources in destSrcDir and .class files in destClassDir)");
      System.exit(1);
    }
    
    String sourceFile = args[0];
    String packageName = args[1];
    String destDir = args[2];
    String classDir = args.length > 3 ? args[3] : null;
    
    IDLCompiler compiler = new IDLCompiler();
    ModuleDef mod = compiler.parse(sourceFile);
      
    CodeGenerator cg = new CodeGenerator();
    cg.destDir = destDir;
    cg.generate(mod, packageName, destDir);
    cg.generateStores(compiler.getStores());

    if (classDir != null) {
      new File(classDir).mkdir();
      ArrayList<String> cmdLine = new ArrayList<String>();
      cmdLine.add("-bootclasspath");
      cmdLine.add(System.getProperty("java.class.path")+File.pathSeparator
        +System.getProperty("sun.boot.class.path"));
      cmdLine.add("-d");
      cmdLine.add(classDir);
      cmdLine.add("-unchecked");
      cmdLine.addAll(cg.sourceFiles);
      try {
        System.out.print("gjc ");
        for (int i = 0; i < cmdLine.size(); i++) System.out.print(cmdLine.get(i)+" ");
        System.out.println();
        gjc.Main.main((String[]) cmdLine.toArray(new String[cmdLine.size()]));
      } catch (Throwable t) {
        System.err.println("Error while trying to compile sources:");
        t.printStackTrace();
      }
    }
  }
  
  protected boolean allowUnknownTypes() { return false; }

  void generateStores(List<StoreDef> stores) throws IOException {
    for (Iterator<StoreDef> iStore = stores.iterator(); iStore.hasNext(); ) {
      StoreDef store = iStore.next();
      String storeName = stripPackage(store.name);
      
      PrintWriter w = getWriter(storeName);
      w.println(fileHeader);
      w.println("public class "+storeName+" extends "+cDiskStore+" implements "+cStore+" {");

      for (int iTypes = 0; iTypes < 2; iTypes++) {
        String type = iTypes == 0 ? "java.io.File" : "String";
        String dir =  iTypes == 0 ? "dir" : "new java.io.File(dir)";
        
        w.println("  public static "+storeName+" open("+type+" dir) {");
        w.println("    return ("+storeName+") "+cStoreRegistry+".openStore("+dir+", "+storeName+".class, false);");
        w.println("  }");
        w.println("  ");
        w.println("  public static "+storeName+" create("+type+" dir) {");
        w.println("    return ("+storeName+") "+cStoreRegistry+".createEmptyStore("+dir+", "+storeName+".class);");
        w.println("  }");
        w.println("  ");
        w.println("  public static "+storeName+" openReadOnly("+type+" dir) {");
        w.println("    return ("+storeName+") "+cStoreRegistry+".openStore("+dir+", "+storeName+".class, true);");
        w.println("  }");
        w.println("  ");
      }

      w.println("  ");
      w.println("  /** don't call this method, use open(), create() or openReadOnly() */");
      w.println("  public "+storeName+"("+cDisk+" disk, boolean readOnly) {");
      w.println("    super(disk, readOnly);");
      w.println("    if (!readOnly) {");
      w.println("    "+cSnapshot+" snapshot = mutableSnapshot();");
      for (int i = 0; i < store.tables.size(); i++) {
        TableDef table = store.tables.get(i);
        w.println("    "+table.name+"(snapshot);");
      }
      w.println("    snapshot.commit();");
      w.println("  }");
      w.println("  }");

      for (int i = 0; i < store.tables.size(); i++) {
        TableDef table = store.tables.get(i);
        TypeDef type = findType(table.type);
        
        if (type == null)
          throw new BadUseException("Unknown struct: "+table.type);
        
        w.println("  ");
        w.println("  public "+cTable+"<"+table.type+"> "+table.name+"("+cSnapshot+" snapshot) {");
        w.println("    "+cTable+"<"+table.type+"> table = snapshot.getTable(\""+table.name+"\", "+table.type+".getTypeInfo());");
        boolean autoinc = false;
        for (int j = 0; j < type.contents.size(); j++) {
          TypeComponent tc = type.contents.get(j);
          if (tc.hints.contains("autoincrement")) {
            if (autoinc) throw new BadUseException("Only one autoincrement field allowed per table");
            w.println("    table.setAutoIncrementField("+table.type+".f_"+tc.name+");");
            autoinc = true;
          }
        }
        w.println("    return table;");
        w.println("  }");
      }

      // close class
      
      w.println("}");
      w.close();
    }
  }
}

