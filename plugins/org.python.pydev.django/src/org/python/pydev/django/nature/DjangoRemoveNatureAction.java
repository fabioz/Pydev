package org.python.pydev.django.nature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.actions.project.PyRemoveNature;

public class DjangoRemoveNatureAction extends PyRemoveNature{
	
    public void run(IAction action) {
        if(selectedProject == null){
            return;
        }
        
        if (!MessageDialog.openConfirm(null, "Confirm Remove Django Nature", StringUtils.format(
        		"Are you sure that you want to remove the Django nature from %s?", selectedProject.getName()))) {
        	return;
        }
        
        try {
			DjangoNature.removeNature(selectedProject, null);
		} catch (CoreException e) {
			PydevPlugin.log(e);
		}
    }

}
