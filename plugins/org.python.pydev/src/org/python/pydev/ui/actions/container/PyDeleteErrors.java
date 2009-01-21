package org.python.pydev.ui.actions.container;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IObjectActionDelegate;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Action used to delete the error markers
 *  
 * @author Fabio
 */
public class PyDeleteErrors extends PyContainerAction implements IObjectActionDelegate {
    

    /**
     * Deletes the error markers... recursively pass the folders and delete the files (and sum them so that we know how many
     * files were affected).
     * 
     * @param container the folder from where we want to remove the markers
     * @return the number of markers deleted
     */
    protected int doActionOnContainer(IContainer container) {
        try {
            container.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
        
        try{
            container.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
            
        return -1;
    }

    @Override
    protected void afterRun(int deleted) {
    }

    @Override
    protected boolean confirmRun() {
        return MessageDialog.openConfirm(null, "Confirm deletion", "Are you sure that you want to recursively remove all the markers from the selected folder(s)?");
    }





}
