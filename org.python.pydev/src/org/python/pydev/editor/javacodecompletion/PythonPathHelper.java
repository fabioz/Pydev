/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is not a singleton because we may have a different pythonpath for each project (even though
 * we have a default one as the original pythonpath).
 * 
 * @author Fabio Zadrozny
 */
public class PythonPathHelper {
    
    /**
     * This is a list of Files containg the pythonpath.
     */
    public List pythonpath = new ArrayList();
    
    /**
     * 
     * @param module - this is a string representing a python module. (xxx.ccc.ccc)
     */
    public File resolveModule(String module){
        return null;
    }
}
