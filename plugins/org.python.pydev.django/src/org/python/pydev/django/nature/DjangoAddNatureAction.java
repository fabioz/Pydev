package org.python.pydev.django.nature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.actions.project.PyAddNature;

public class DjangoAddNatureAction extends PyAddNature{
	
    public void run(IAction action) {
        if(selectedProject == null){
            return;
        }
        
        try {
            DjangoNature.addNature(selectedProject, null);
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
        //TODO: Set the manage.py location if not set.
    }

}
