/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;
import java.io.Serializable;

/**
 * This class defines the key to use for some module. All its operations are based on its name.
 * The file may be null.
 * 
 * @author Fabio Zadrozny
 */
public class ModulesKey implements Comparable, Serializable{

    private static final long serialVersionUID = 1L;
    public String name;
    public File file;

    /**
	 * Creates the module key. File may be null
	 */
	public ModulesKey(String name, File f) {
	    this.name = name;
	    this.file = f;
	}

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        if (o instanceof ModulesKey ){
            ModulesKey m = (ModulesKey)o;
            
            return name.compareTo(m.name);
        }
        return 0;
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
        
        return true;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.name.hashCode();
    }
	
}
