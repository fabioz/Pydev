/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.JobProgressComunicator;

/**
 * This structure should be in memory, so that it acts very quickly.
 * 
 * Probably an hierarchical structure where modules are the roots and they 'link' to other modules or other definitions, would be what we
 * want.
 * 
 * The ast manager is a part of the python nature (as a field).
 * 
 * @author Fabio Zadrozny
 */
public class ASTManager implements Serializable {

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are strings with the name of the module.
     * Values are AbstractModule objects.
     */
    private Map modules = new HashMap();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    private PythonPathHelper pythonPathHelper = new PythonPathHelper();

    /**
     * We can save the completions to some stream, so that in a later time it can be restored from it (which should be faster than parsing
     * everything again)
     * 
     * @param out
     * @param monitor
     */
    public void saveASTManager(OutputStream out, JobProgressComunicator monitor) {
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            try {
                int size = modules.size();
                stream.writeInt(size);
                monitor.worked("Saving completions to disk: 0 of "+ size, 1);

                int j = 0;
                for (Iterator i = modules.entrySet().iterator(); i.hasNext()  && monitor.isCanceled() == false; j++) {
                    Map.Entry e = (Map.Entry) i.next();
                    Object key = e.getKey();
                    stream.writeObject(key);
                    stream.writeObject(e.getValue());
                    monitor.worked(new StringBuffer("Saving completion to disk: ").append(j).append(" of ").append(size).append(" (").append(key).append(")").toString(), 1);
                }

                if( monitor.isCanceled() == false){
	                stream.writeObject(pythonPathHelper);
	                monitor.worked("Saved all", 1);
                }
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            PydevPlugin.log(e);
        }
    }

    /**
     * Restores the completions from a stream, so that we can use its completions again.
     * 
     * @param in
     * @param monitor
     * @param job
     */
    public static ASTManager restoreASTManager(InputStream in, IProgressMonitor monitor, Job job) {
        try {
            ObjectInputStream stream = new ObjectInputStream(in);
            try {
                ASTManager c = new ASTManager();
                int size = stream.readInt();
                JobProgressComunicator commun = new JobProgressComunicator(monitor, "Reading completions from disk", size +2, job);
                
                commun.worked("Reading completions from disk: 0 of "+size, 1);
                
                c.modules = new HashMap();
                for (int i = 0; i < size && monitor.isCanceled() == false; i++) {
                    Object key = stream.readObject();
                    Object value = stream.readObject();
                    c.modules.put(key, value);
                    
                    commun.worked(new StringBuffer("Reading completions from disk: ").append(i).append(" of ").append(size).append(" (").append(key).append(")").toString(), 1);
                }
                
                if( monitor.isCanceled() == false){
	                c.pythonPathHelper = (PythonPathHelper) stream.readObject();
	                commun.worked("Read all completions from disk.",1);
	                commun.done();
	                return c;
                }
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                Object[] objs1 = (Object[]) o1;
                Object[] objs2 = (Object[]) o2;
                if(objs1[0] == null || objs2[0] == null){
                    return 0;
                }
                return ((String) (objs1)[0]).compareTo((objs2)[0]);
            }
        };
        return comparator;
    }

    /**
     * This function rebuilds the completions based on the pythonpath passed.
     * 
     * @param pythonpath - string with the pythonpath (separated by |)
     * @param monitor
     */
    public void rebuildModules(String pythonpath, IProgressMonitor monitor) {
        System.out.println("rebuildModules " + pythonpath);

        List pythonpathList = pythonPathHelper.setPythonPath(pythonpath);

        Map mods = new HashMap();
        
        List completions = new ArrayList();
        
        int total = 0;
        
        //first thing: get all files available from the python path and sum them up.
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = (String) iter.next();

            //the slow part is getting the files... not much we can do (I think).
            List[] below = pythonPathHelper.getModulesBelow(new File(element), monitor);
            completions.addAll(below[0]);
            total += below[0].size();
        }

    
        int j = 0;
        //now, create in memory modules for all the loaded files.
        for (Iterator iterator = completions.iterator(); iterator.hasNext() && monitor.isCanceled() == false; j++) {
            Object o = iterator.next();
            if(o instanceof File){
	            File f = (File) o;
	            String m = pythonPathHelper.resolveModule(f.getAbsolutePath());
	
	            monitor.setTaskName(new StringBuffer("Creating completion: ").append(j).append(" of ").append(total).append(" (").append(m).append(")").toString());
	            monitor.worked(1);
	
	            if (m != null) {
	                AbstractModule s = AbstractModule.createModule(f, pythonPathHelper);
	                mods.put(m, s);
	            }
            }
        }
        modules = mods;

    }

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a TreeSet with the imports as tuples with the name and the docstring.
     */
    public Set getCompletionForImport(final String original) {
        String initial = original;
        if (initial.endsWith(".")) {
            initial = initial.substring(0, initial.length() - 1);
        }
        initial = initial.toLowerCase().trim();

        //set to hold the completion (no duplicates allowed).
        Set l = new TreeSet(getComparator());

        //first we get the imports...
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            String element = (String) iter.next();

            if (element.toLowerCase().startsWith(initial)) {
                element = element.substring(initial.length());

                if (element.startsWith(".")) {
                    element = element.substring(1);
                }

                if(element.length() > 0){
	                String[] splitted = element.split("\\.");
	                if (splitted.length > 0) {
	                    //new String[]{token, description}
	                    l.add(new Object[] { splitted[0], "", new Integer(PyCodeCompletion.TYPE_IMPORT) });
	                }
                }
            }
        }

        //Now, if we have an initial module, we have to get the completions
        //for it.
        if (initial.length() > 0) {
            String nameInCache = original;
            if (nameInCache.endsWith(".")) {
                nameInCache = nameInCache.substring(0, nameInCache.length() - 1);
            }

            Object object = modules.get(nameInCache);
            if (object instanceof AbstractModule) {
                AbstractModule m = (AbstractModule) object;
                List globalTokens = m.getGlobalTokens();
                for (Iterator iter = globalTokens.iterator(); iter.hasNext();) {
                    IToken element = (IToken) iter.next();
                    l.add(new Object[] { element.getRepresentation(), element.getDocStr(), new Integer(element.getCompletionType()) });

                }
            }

        }

        return l;
    }

    /**
     * @return a Set of strings with all the modules.
     */
    public Set getAllModules() {
        return modules.keySet();
    }

    /**
     * @return
     */
    public int getSize() {
        return modules.size();
    }
}