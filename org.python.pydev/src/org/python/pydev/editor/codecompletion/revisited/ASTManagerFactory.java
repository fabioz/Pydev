/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileInputStream;
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
public class ASTManagerFactory {


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

        File file = ASTManager.getPythonPathHelperFilePath(dir);

        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                try {

                    ObjectInputStream in = new ObjectInputStream(stream);
                    try {
                        c.pythonPathHelper = (PythonPathHelper) in.readObject();
                        int size = in.readInt();
                        
                        for (int i=0; i<size; i++) {
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
                File filePath = ASTManager.getPythonPathHelperFilePath(parentDir);
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


}