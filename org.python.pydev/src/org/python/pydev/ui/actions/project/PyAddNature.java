package org.python.pydev.ui.actions.project;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Adds a nature to the given selected project.
 * 
 * @author Fabio
 */
public class PyAddNature extends PyRemoveNature{

    public void run(IAction action) {
        if(selectedProject == null){
            return;
        }
        
        try {
            PythonNature.addNature(selectedProject, null, null, null);
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
    }

}
