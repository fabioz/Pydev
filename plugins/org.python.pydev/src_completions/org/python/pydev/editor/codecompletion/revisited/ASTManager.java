/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.IASTManagerObserver;
import org.python.pydev.plugin.PydevPlugin;


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
public class ASTManager extends AbstractASTManager implements ICodeCompletionASTManager, Serializable{

    
    /**
     * changed to 11L on release 1.4.2
     */
    protected static final long serialVersionUID = 11L;
    
    public ASTManager() {}
    
    /**
     * Set the project this ast manager works with.
     */
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas){
        getProjectModulesManager().setProject(project, nature, restoreDeltas);
        List<IASTManagerObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_MANAGER_OBSERVER);
        for (IASTManagerObserver observer : participants) {
            try {
                observer.notifyASTManagerAttached(this);
            } catch (Exception e) {
                //let's keep it safe
                PydevPlugin.log(e);
            }
        }
    }

    public IModulesManager getModulesManager(){
        return getProjectModulesManager();
    }

    /**
     * @return modules manager wrapped cast to the interface we expect. Creates it if needed.
     */
    private synchronized IProjectModulesManager getProjectModulesManager() {
        if(modulesManager == null){
            modulesManager = new ProjectModulesManager();
        }
        return (IProjectModulesManager) modulesManager;
    }
    
    
    //----------------------- AUXILIARIES


    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        getProjectModulesManager().changePythonPath(pythonpath, project, monitor);
    }
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, IPythonNature nature) {
        getProjectModulesManager().rebuildModule(f, doc, project, monitor, nature);
    }
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        getProjectModulesManager().removeModule(file, project, monitor);
    }

    /**
     * @return
     */
    public int getSize() {
        return getProjectModulesManager().getSize(true);
    }

    public static ICodeCompletionASTManager loadFromFile(File astOutputFile) {
        return (ICodeCompletionASTManager) IOUtils.readFromFile(astOutputFile);
    }

}



class IOUtils {

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            //ok, no need to log it.
            return null;
        }
    }

}