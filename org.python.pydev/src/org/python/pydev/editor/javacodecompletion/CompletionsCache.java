/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    
    private PythonPathHelper pythonPathHelper = new PythonPathHelper();
    
    
    /**
     * This function rebuilds the completions based on the pythonpath passed. 
     * 
     * @param pythonpath - string with the pythonpath (separated by commas)
     */
    public void rebuildModules(String pythonpath){
        List pythonpathList = pythonPathHelper.setPythonPath(pythonpath);
        
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            
            //the slow part is getting the files... not much we can do (I think).
            List[] below = pythonPathHelper.getModulesBelow(new File(element));
            
            for (Iterator iterator = below[0].iterator(); iterator.hasNext();) {
                File f = (File) iterator.next();
                String m = pythonPathHelper.resolveModule(f.getAbsolutePath());
                
                if (m != null){
                    modules.put(m, f);
                }
            }
        }
    }

    /**
     * 
     */
    public Set getImports() {
        Set l = new TreeSet();
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            String[] splitted = element.split("\\.");
            if(splitted.length > 0){
                l.add(splitted[0]);
            }
        }
        return l;
    }
    
    public Set getImports(String initial){
        Set l = new TreeSet();
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            
            if(element.startsWith(initial)){
                element = element.substring(initial.length());
                
                if(element.startsWith(".")){
                    element = element.substring(1);
                    
    	            String[] splitted = element.split("\\.");
    	            if(splitted.length > 0){
    	                l.add(splitted[0]);
    	            }
                }
            }
        }
        return l;
    }
    
    public Set getAllModules(){
        return modules.keySet();
    }
}