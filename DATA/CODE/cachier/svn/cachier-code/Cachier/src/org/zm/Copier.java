package org.zm;

import java.io.File;

public interface Copier extends SystemComponent {

    /**
     * Copies recursively the file/directory at source to the file/directory at
     * location
     * 
     * Examples copy("C:/aFile", "C:/aNotherFile"); copies file to file
     * copy("C:/aFile", "C:/aDirectory/"); copies file INTO directory
     * copy("C:/aDirectory/", "C:/aNotherDirectory/"); copies directory INTO
     * another directory result = C:/aNotherDirectory/aDirectory/
     * 
     * @param source
     * @param destination
     * @return
     */
    public boolean copy(File source, File destination);

}
