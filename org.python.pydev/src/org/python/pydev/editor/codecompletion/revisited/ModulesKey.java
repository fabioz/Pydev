/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.Serializable;

/**
 * @author Fabio Zadrozny
 */
public class ModulesKey implements Comparable, Serializable{
    public String name;
    public File file;

    /**
	 * 
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
            
            int i = name.compareTo(m.name);
            if (i != 0)
                return i;
            
            i = file.compareTo(m.file);
            return i;
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
