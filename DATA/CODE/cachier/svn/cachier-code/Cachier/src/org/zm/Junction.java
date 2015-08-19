package org.zm;

import java.io.File;
import java.io.IOException;

public class Junction implements Linker {

    String junctionPath = "junction.exe";

    @Override
    public boolean link(File source, File destination) {
	try {
	    Runtime.getRuntime().exec(junctionPath + " \"" + destination.getAbsolutePath() + "\" \"" + source.getAbsolutePath() + "\"");
	} catch (IOException e) {
	    return false;
	}
	return true;
    }

    @Override
    public boolean isAvailable() {
	Process p;
	try {
	    p = Runtime.getRuntime().exec(junctionPath + "-v");

	    p.waitFor();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    return false;
	}

	return p.exitValue() == 0;
    }

    public Junction() {
	super();
    }

    public Junction(String junctionPath) {
	super();
	this.junctionPath = junctionPath;
    }

}
