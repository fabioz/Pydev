package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;

public abstract class ProjectModulesManagerBuild extends ModulesManager implements IProjectModulesManager{
    

    /**
     * @see org.python.pydev.core.ICodeCompletionASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        if(file == null){
            return;
        }
        
        if (file.isDirectory()) {
            removeModulesBelow(file, project, monitor);

        } else {
            if(file.getName().startsWith("__init__.")){
                removeModulesBelow(file.getParentFile(), project, monitor);
            }else{
                removeModulesWithFile(file);
            }
        }
    }
    

    /**
     * @param file
     */
    private void removeModulesWithFile(File file) {
        if(file == null){
            return;
        }
        
        List<ModulesKey> toRem = new ArrayList<ModulesKey>();
        synchronized (modulesKeys) {
    
            for (Iterator iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
                ModulesKey key = (ModulesKey) iter.next();
                if (key.file != null && key.file.equals(file)) {
                    toRem.add(key);
                }
            }
    
            removeThem(toRem);
        }
    }

    /**
     * removes all the modules that have the module starting with the name of the module from
     * the specified file.
     */
    private void removeModulesBelow(File file, IProject project, IProgressMonitor monitor) {
        if(file == null){
            return;
        }
        
        String absolutePath = REF.getFileAbsolutePath(file);
        List<ModulesKey> toRem = new ArrayList<ModulesKey>();
        
        synchronized (modulesKeys) {

            for (ModulesKey key : modulesKeys.keySet()) {
                if (key.file != null && REF.getFileAbsolutePath(key.file).startsWith(absolutePath)) {
                    toRem.add(key);
                }
            }
    
            removeThem(toRem);
        }
    }


    // ------------------------ building
    
    /**
     * @see org.python.pydev.core.ICodeCompletionASTManager#rebuildModule(java.io.File, org.eclipse.jface.text.IDocument,
     *      org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, IPythonNature nature) {
        final String m = pythonPathHelper.resolveModule(REF.getFileAbsolutePath(f));
        if (m != null) {
            addModule(new ModulesKey(m, f));

            
        }else if (f != null){ //ok, remove the module that has a key with this file, as it can no longer be resolved
            synchronized (modulesKeys) {
                Set<ModulesKey> toRemove = new HashSet<ModulesKey>();
                for (Iterator iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
                    ModulesKey key = (ModulesKey) iter.next();
                    if(key.file != null && key.file.equals(f)){
                        toRemove.add(key);
                    }
                }
                removeThem(toRemove);
            }
        }
    }



}
