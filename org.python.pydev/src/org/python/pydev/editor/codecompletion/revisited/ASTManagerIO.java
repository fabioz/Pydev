/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class ASTManagerIO {


    /**
     * @param monitor
     * @see other function. This is a wrapper that uses a file.
     * 
     * @param file
     * @param monitor
     * @param string
     * @param job
     * @return
     */
    public static IASTManager restoreASTManager(IProject p, String pythonpath, IProgressMonitor monitor) {
        File dir = ASTManager.getCompletionsCacheDir(p);
        ASTManager c = new ASTManager();

        File file = ASTManagerIO.getPythonPathHelperFilePath(dir);

        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                try {

                    ObjectInputStream in = new ObjectInputStream(stream);
                    try {
                        c.pythonPathHelper = (PythonPathHelper) in.readObject();
                        int size = in.readInt();
                        
                        for (int i=0; i<size; i++) {
                            //create all modules as empty modules.
                            ModulesKey element = (ModulesKey) in.readObject();
                            c.modules.put(element, AbstractModule.createEmptyModule(element.name, element.file));
                            
                        }
                    } finally {
                        in.close();
                    }

                } finally {
                    stream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                PydevPlugin.log(e);
                
                if (pythonpath != null){
                    c.changePythonPath(pythonpath, p, monitor);
                }
            }
        }
        
        return c;

    }

    /**
     * Save the pythonpath helper.
     * 
     * @see other function.
     * 
     * @param parentDir: directory where the files should be saved.
     * @param monitor
     */
    static void savePythonPath(File parentDir, IProgressMonitor monitor, ASTManager ast) {
        try {
            int size = ast.modules.size()+1;
            monitor.worked(1);
            monitor.setTaskName("Saving python path to disk...");
    
            if (monitor.isCanceled() == false) {
                File filePath = ASTManagerIO.getPythonPathHelperFilePath(parentDir);
                FileOutputStream out = new FileOutputStream(filePath);
    
                try {
                    ObjectOutputStream stream = new ObjectOutputStream(out);
                    try {
                        stream.writeObject(ast.pythonPathHelper);
                        Set s = ast.modules.keySet();
                        stream.writeInt(s.size());
                        for (Iterator iter = s.iterator(); iter.hasNext();) {
                            Object element = (Object) iter.next();
	                        stream.writeObject(element);
                        }
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
            e.printStackTrace();
            PydevPlugin.log(e);
        }
    
    }

    /**
     * This saves an object representing a delta to a file.
     * 
     * TODO: right now, deltas are not being saved.
     * 
     * @param f - File to save the delta (the module should have the same name).
     * @param obj tuple so that the first item is the name of the module and the second the module itself.
     */
    static void saveDelta(File f, ModulesKey key, Object value) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                ObjectOutputStream stream = null;
                try {
                    stream = new ObjectOutputStream(out);
                    stream.writeObject(key);
                    stream.writeObject(value);
    
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

    /**
     * TODO: right now, deltas are not being saved.
     * 
     * @param monitor
     * @param deltas
     * @param c
     * @return
     */
    static void restoreDelta(IProgressMonitor monitor, File delta, ASTManager manager) {
    
        monitor.worked(1);
        monitor.setTaskName(new StringBuffer("Restoring delta: ").append(delta).toString());
    
        Object value = null;
        File file = delta;
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                try {
    
                    ObjectInputStream stream = null;
    
                    try {
                        stream = new ObjectInputStream(in);
                        ModulesKey key = (ModulesKey) stream.readObject();
                        value = stream.readObject();
                        if (value != null) {
                            manager.modules.put(key, value);
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

    /**
     * @param dir: parent directory where file should be.
     * @return the file where the python path helper should be saved.
     */
    static File getPythonPathHelperFilePath(File dir) {
        return new File(dir, "pathhelper" + ".pydevpathhelper");
    }

    /**
     * @param dir: parent directory where file should be.
     * @param name: name of the file.
     * @return the file where the module with name "name" should be saved.
     */
    private static File getFilePath(File dir, String name) {
        return new File(dir, name + ".pydevcompletions");
    
    }


}