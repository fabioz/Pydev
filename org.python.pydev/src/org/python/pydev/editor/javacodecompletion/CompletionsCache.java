/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.util.HashMap;
import java.util.Map;

/**
 * This structure should be in memory, so that it acts very quicly. 
 * 
 * Probably an hierarchical structure where modules are the roots and they
 * 'link' to other modules or other definitions, would be what we want.
 * 
 * @author Fabio Zadrozny
 */
public class CompletionsCache {

    public Map modules = new HashMap();
    
    public void addModule(String modName, SourceModule mod){
        
    }
}