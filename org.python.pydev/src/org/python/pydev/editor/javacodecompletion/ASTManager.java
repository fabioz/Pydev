/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This structure should be in memory, so that it acts very quickly. 
 * 
 * Probably an hierarchical structure where modules are the roots and they
 * 'link' to other modules or other definitions, would be what we want.
 * 
 * The ast manager is a part of the python nature (as a field). 
 * 
 * @author Fabio Zadrozny
 */
public class ASTManager implements Serializable{

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are strings with the name of the module.
     */
    public Map modules = new HashMap();
    
    /**
     * Helper for using the pythonpath. Also persisted.
     */
    private PythonPathHelper pythonPathHelper = new PythonPathHelper();

    
    /**
     * We can save the completions to some stream, so that in a later time
     * it can be restored from it (which should be faster than parsing everything again)
     * @param out
     */
    public void saveASTManager(OutputStream out){
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            try{
                stream.writeObject(this);
            }finally{
                stream.close();
            }
        } catch (IOException e) {
            PydevPlugin.log(e);
        }
    }
    
    /**
     * Restores the completions from a stream, so that we can use its completions again.
     * @param in
     */
    public static ASTManager restoreASTManager(InputStream in){
        try {
            ObjectInputStream stream = new ObjectInputStream(in);
            try{
                ASTManager c = (ASTManager) stream.readObject();
                return c;
            }finally{
                stream.close();
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
    }
    
    /**
     * Warning: may throw Class cast exceptions if misused.
     * 
     * @return a comparator for tuples that have a string as the first item.
     */
    private Comparator getComparator() {
        Comparator comparator = new Comparator(){
            public int compare(Object o1, Object o2) {
                return ((String)((Object[])o1)[0]).compareTo(((Object[])o2)[0]);
            }};
        return comparator;
    }
    
    /**
     * This function rebuilds the completions based on the pythonpath passed. 
     * 
     * @param pythonpath - string with the pythonpath (separated by |)
     */
    public void rebuildModules(String pythonpath){
        System.out.println("rebuildModules "+ pythonpath);
        
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
        System.out.println("found modules: "+modules.size());
    }



    /**
     * Returns the imports that start with a given string. The comparisson is not
     * case dependent.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a TreeSet with the imports as tuples with the name and the docstring.
     */
    public TreeSet getImports(String initial){
        if(initial.endsWith(".")){
            initial = initial.substring(0, initial.length()-1);
        }
        initial = initial.toLowerCase().trim();
        
        TreeSet l = new TreeSet(getComparator());
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            
            if(element.toLowerCase().startsWith(initial)){
                element = element.substring(initial.length());
                
                if(element.startsWith(".")){
                    element = element.substring(1);
                }

                String[] splitted = element.split("\\.");
	            if(splitted.length > 0){
	                //new String[]{token, description}
	                l.add(new Object[]{splitted[0], "", new Integer(PyCodeCompletion.TYPE_IMPORT)});
	            }
            }
        }
        return l;
    }
    
    /**
     * @return a Set of strings with all the modules.
     */
    public Set getAllModules(){
        return modules.keySet();
    }
}