/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.utils.JobProgressComunicator;

/**
 * This structure should be in memory, so that it acts very quickly.
 * 
 * Probably an hierarchical structure where modules are the roots and they 'link' to other modules or other definitions, would be what we
 * want.
 * 
 * The ast manager is a part of the python nature (as a field).
 * 
 * TODO: OK, saving is a one step and then on demand, but we could do lazy evaluation so that we don't have too 
 * much time to start using the plugin... (just load module names and do the rest as requested).
 * 
 * @author Fabio Zadrozny
 */
public class ASTManager implements Serializable {

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are strings with the name of the module. Values are AbstractModule objects.
     */
    private Map modules = new HashMap();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    private PythonPathHelper pythonPathHelper = new PythonPathHelper();

    //----------------------- SAVE

    /**
     * @param dir: parent directory where file should be.
     * @param name: name of the file.
     * @return the file where the module with name "name" should be saved.
     */
    private File getFilePath(File dir, String name) {
        return new File(dir, name + ".pydevcompletions");

    }

    /**
     * @param dir: parent directory where file should be.
     * @return the file where the python path helper should be saved.
     */
    private static File getPythonPathHelperFilePath(File dir) {
        return new File(dir, "pathhelper" + ".pydevpathhelper");
    }

    /**
     * Saves the ast to a file.
     * 
     * @see other function.
     * 
     * @param parentDir: directory where the files should be saved.
     * @param monitor
     */
    public void saveASTManager(File parentDir, IProgressMonitor monitor) {
        try {
            int size = modules.size()+1;
            monitor.worked(1);
            monitor.setTaskName("Saving completions to disk: 0 of " + size);

            int j = 0;
            for (Iterator i = modules.entrySet().iterator(); i.hasNext() && monitor.isCanceled() == false; j++) {

                Map.Entry e = (Map.Entry) i.next();
                Object key = e.getKey();
                File filePath = getFilePath(parentDir, (String) key);

                if (filePath.exists() == false) {
                    filePath.createNewFile();
                }
                FileOutputStream out = new FileOutputStream(filePath);

                try {
                    ObjectOutputStream stream = new ObjectOutputStream(out);
                    try {
                        stream.writeObject(key);
                        stream.writeObject(e.getValue());
                        monitor.worked(1);
                        monitor.setTaskName(new StringBuffer("Saving completion to disk: ").append(j).append(" of ").append(size).append(
                                " (").append(key).append(")").toString());
                    } finally {
                        stream.close();
                    }
                } finally {
                    out.close();
                }
            }

            if (monitor.isCanceled() == false) {
                File filePath = getPythonPathHelperFilePath(parentDir);
                FileOutputStream out = new FileOutputStream(filePath);

                try {
                    ObjectOutputStream stream = new ObjectOutputStream(out);
                    try {
                        stream.writeObject(pythonPathHelper);
                        monitor.worked(1);
                        monitor.setTaskName("Saved all");
                    } finally {
                        stream.close();
                    }
                } finally {
                    out.close();
                }
            }
        } catch (IOException e) {
            PydevPlugin.log(e);
        }

    }

    /**
     * This saves an object representing a delta to a file.
     * 
     * @param f
     * @param obj tuple so that the first item is the name of the module and the second the module itself.
     */
    private void saveDelta(File f, Object[] obj) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                ObjectOutputStream stream = null;
                try {
                    stream = new ObjectOutputStream(out);
                    stream.writeObject(obj[0]);
                    stream.writeObject(obj[1]);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            } finally {
                try {
                    out.close();
                } catch (IOException e1) {
                    //that should be ok.
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            PydevPlugin.log(e);
        }
    }

    //----------------------- RESTORE

    /**
     * @see other function. This is a wrapper that uses a file.
     * 
     * @param file
     * @param monitor
     * @param job
     * @return
     */
    public static ASTManager restoreASTManager(File dir, IProgressMonitor monitor, Job job) {
        ASTManager c = new ASTManager();

        File[] deltas = dir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith("pydevcompletions");
            }

        });


        if (monitor.isCanceled() == false) {
            File file = getPythonPathHelperFilePath(dir);

            if (file.exists()) {
                try {
                    FileInputStream stream = new FileInputStream(file);
                    try {

                        ObjectInputStream in = new ObjectInputStream(stream);
                        try {
                            c.pythonPathHelper = (PythonPathHelper) in.readObject();
                        } finally {
                            in.close();
                        }

                    } finally {
                        stream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    PydevPlugin.log(e);
                }
            }
            
            //first we restore the pythonpath helper because it was the last saved.
            //the deltas are restored later.
            if(c.pythonPathHelper != null){
		        restoreDeltas(monitor, deltas, c, job);
		        monitor.done();
            }
            
            
            return c;
        }
        return null;

    }

    /**
     * @param monitor
     * @param deltas
     * @param c
     * @return
     */
    private static IProgressMonitor restoreDeltas(IProgressMonitor monitor, File[] deltas, ASTManager c, Job j) {
        int size = deltas.length;
        
        monitor = new JobProgressComunicator(monitor, "Restoring", size+1, j);
        
        for (int i = 0; i < deltas.length && monitor.isCanceled() == false; i++) {
            String curr = deltas[i].getName();
            monitor.worked(1);
            monitor.setTaskName(new StringBuffer("Restoring deltas: ").append(i).append(" of ").append(size).append(" (").append(curr)
                    .append(")").toString());

            Object value = null;
            File file = deltas[i];
            if (file.exists()) {
                try {
                    FileInputStream in = new FileInputStream(file);
                    try {

                        ObjectInputStream stream = null;

                        try {
                            stream = new ObjectInputStream(in);
				            String key = (String) stream.readObject();
                            value = stream.readObject();
				            if (value != null) {
				                c.modules.put(key, value);
				            }
                        } finally {
                            if (stream != null) {
                                stream.close();
                            }
                        }

                    } finally {
                        in.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    PydevPlugin.log(e);
                }
            }

        }
        return monitor;
    }

    //--------------------------------- METHODS TO REBUILD

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
            if (o instanceof File) {
                File f = (File) o;
                String m = pythonPathHelper.resolveModule(f.getAbsolutePath());

                monitor.setTaskName(new StringBuffer("Creating completion: ").append(j).append(" of ").append(total).append(" (").append(m)
                        .append(")").toString());
                monitor.worked(1);

                if (m != null) {
                    AbstractModule s = AbstractModule.createModule(m, f);
                    mods.put(m, s);
                }
            }
        }
        modules = mods;

    }

    /**
     * This method is an interface
     * 
     * @param monitor
     * @param f
     */
    public void rebuildModule(final File f, final IDocument doc, final IProject project) {
        final String m = pythonPathHelper.resolveModule(f.getAbsolutePath());
        if (m != null) {
            final AbstractModule s = AbstractModule.createModuleFromDoc(m, f, doc);
            modules.put(m, s);

            new Thread() {
                public void run() {
                    System.out.println("Saving delta");
                    File dir = PythonNature.getCompletionsCacheDir(project);
                    String name = m;
                    File f = new File(dir, name + ".pydevcompletions");
                    saveDelta(f, new Object[] { m, s });
                }
            }.start();
        }
    }

    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public IToken[] getCompletionsForImport(final String original) {
        String initial = original;
        if (initial.endsWith(".")) {
            initial = initial.substring(0, initial.length() - 1);
        }
        initial = initial.toLowerCase().trim();

        //set to hold the completion (no duplicates allowed).
        Set set = new HashSet();

        //first we get the imports...
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            String element = (String) iter.next();

            if (element.toLowerCase().startsWith(initial)) {
                element = element.substring(initial.length());

                boolean goForIt = false;
                //if initial is not empty only get those that start with a dot (submodules, not
                //modules that start with the same name).
                //e.g. we want xml.dom
                //and not xmlrpclib
                if (initial.length() != 0) {
                    if (element.startsWith(".")) {
                        element = element.substring(1);
                        goForIt = true;
                    }
                } else {
                    goForIt = true;
                }

                if (element.length() > 0 && goForIt) {
                    String[] splitted = element.split("\\.");
                    if (splitted.length > 0) {
                        //this is the completion
                        set.add(new ConcreteToken(splitted[0], "", initial, PyCodeCompletion.TYPE_IMPORT));
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

                IToken[] globalTokens = m.getGlobalTokens();
                for (int i = 0; i < globalTokens.length; i++) {
                    IToken element = globalTokens[i];
                    //this is the completion
                    set.add(element);
                }
            }
        }

        return (IToken[]) set.toArray(new IToken[0]);
    }

    /**
     * @return a Set of strings with all the modules.
     */
    public String[] getAllModules() {
        return (String[]) modules.keySet().toArray(new String[0]);
    }

    /**
     * @return
     */
    public int getSize() {
        return modules.size();
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param file
     * @return the module represented by the file.
     */
    private AbstractModule getModule(File file) {
        String name = pythonPathHelper.resolveModule(file.getAbsolutePath());
        return getModule(name);
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name
     * @return the module represented by this name
     */
    private AbstractModule getModule(String name) {
        return (AbstractModule) modules.get(name);
    }

    /**
     * The completion should work in the following way:
     * 
     * First we have to know in which scope we are.
     * 
     * If we have no token nor qualifier, get the locals for the file (only from module imports or from inner scope).
     * 
     * If we have a part of the qualifier and not activationToken, go for all that match (e.g. all classes, so that we can make the import
     * automatically)
     * 
     * If we have the activationToken, try to guess what it is and get its attrs and funcs.
     * 
     * @param file
     * @param line
     * @param col
     * @param activationToken
     * @param qualifier
     * @return
     */
    public IToken[] getCompletionsForToken(File file, int line, int col, String activationToken, String qualifier) {
        AbstractModule module = getModule(file); //this is the module we are in.
        return getCompletionsForModule(file.toString(), activationToken, qualifier, module);
    }

    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     */
    private IToken[] getCompletionsForModule(String modName, String activationToken, String qualifier, AbstractModule module) {
        List completions = new ArrayList();

        if (module != null) {

            if (activationToken.length() == 0 && qualifier.length() == 0) {

                //in completion with nothing, just go for what is imported and global tokens.
                IToken[] globalTokens = module.getGlobalTokens();
                for (int i = 0; i < globalTokens.length; i++) {
                    completions.add(globalTokens[i]);
                }

                //now go for the token imports
                IToken[] importedModules = module.getTokenImportedModules();
                for (int i = 0; i < importedModules.length; i++) {
                    completions.add(importedModules[i]);
                }

                //wild imports: recursively go and get those completions.
                IToken[] wildImportedModules = module.getWildImportedModules();
                for (int i = 0; i < wildImportedModules.length; i++) {
                    //                    System.out.println(wildImportedModules[i]);

                    IToken name = wildImportedModules[i];
                    AbstractModule mod = getModule(name.getRepresentation());
                    if (mod != null) {
                        IToken[] completionsForModule = getCompletionsForModule(name.getRepresentation(), activationToken, qualifier, mod);
                        for (int j = 0; j < completionsForModule.length; j++) {
                            completions.add(completionsForModule[j]);
                        }
                    } else {
                        System.out.println("Module not found:" + name.getRepresentation());
                    }
                }

            }
        } else {
            System.out.println("Invalid module: " + modName);
        }
        return (IToken[]) completions.toArray(new IToken[0]);
    }

}