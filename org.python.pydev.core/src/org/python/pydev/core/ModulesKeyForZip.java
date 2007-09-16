package org.python.pydev.core;

import java.io.File;

/**
 * This is the modules key that should be used if we have an entry in a zip file.
 * 
 * @author Fabio
 */
public class ModulesKeyForZip extends ModulesKey{

    /**
     * 1L = just name and file
     * 2L = + zipModulePath
     */
    private static final long serialVersionUID = 2L;
    
    
    /**
     * This should be null if it's from a file in the filesystem, now, if we're dealing with a zip file,
     * the file should be the zip file and this the path under which it was found in the zip file.
     * 
     * Some cases can be considered:
     * - if it was found from jython this is a dir from the zip file
     * - if it was from a zip file from python this is a the .py file path inside the zip file
     */
    public String zipModulePath;
    
    
    /**
     * Creates the module key. File may be null
     */
    public ModulesKeyForZip(String name, File f, String zipModulePath) {
        super(name, f);
        this.zipModulePath = zipModulePath;
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer(name);
        if(file != null){
            ret.append(" - ");
            ret.append(file);
        }
        if(zipModulePath != null){
            ret.append(" - zip path:");
            ret.append(zipModulePath);
        }
        return ret.toString();
    }

}
