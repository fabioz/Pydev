/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.io.File;
import java.io.Serializable;

/**
 * This class defines the key to use for some module. All its operations are based on its name.
 * The file may be null.
 * 
 * @author Fabio Zadrozny
 */
public class ModulesKey implements Comparable<ModulesKey>, Serializable{

    /**
     * 1L = just name and file
     * 2L = + zipModulePath
     */
    private static final long serialVersionUID = 2L;
    
    /**
     * Then name is always needed!
     */
    public String name;
    
    /**
     * Builtins may not have the file (null)
     */
    public File file;
    
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
     * Builtins may not have the file
     */
    public ModulesKey(String name, File f) {
        this(name, f, null);
    }
    
    /**
	 * Creates the module key. File may be null
	 */
	public ModulesKey(String name, File f, String zipModulePath) {
	    this.name = name;
	    this.file = f;
	    this.zipModulePath = zipModulePath;
	}

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ModulesKey o) {
        return name.compareTo(o.name);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof ModulesKey )){
            return false;
        }
        
        ModulesKey m = (ModulesKey)o;
        if(!(name.equals(m.name))){
            return false;
        }
        
        //consider only the name
        return true;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.name.hashCode();
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
