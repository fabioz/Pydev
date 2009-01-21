package org.python.pydev.ui.actions.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Abstract class for actions that'll act upon the selected containers.
 * 
 * @author Fabio
 */
public abstract class PyContainerAction {

    
    /**
     * List with the containers the user selected 
     */
    protected List<IContainer> selectedContainers;

    
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        //empty
    }
    
    
    /**
     * When the selection changes, we've to keep the selected containers...
     */
    @SuppressWarnings("unchecked")
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            selectedContainers = null;
            return;
        }
        
        IStructuredSelection selections = (IStructuredSelection) selection;
        ArrayList<IContainer> containers = new ArrayList<IContainer>();
        
        for(Iterator<Object> it = selections.iterator(); it.hasNext();){
            Object o = it.next();
            if(o instanceof IContainer){
                containers.add((IContainer) o);
            }
        }
        
        this.selectedContainers = containers;
    }
    
    
    /**
     * Act on the selection to do the needed action (will confirm and make a refresh before executing)
     */
    public void run(IAction action) {
        //should not happen
        if(selectedContainers == null){
            return;
        }
        
        if (!confirmRun()){
            return;
        }
        
        int nDeleted = 0;
        IProgressMonitor nullProgressMonitor = new NullProgressMonitor();
        
        for (Iterator<IContainer> iter = this.selectedContainers.iterator(); iter.hasNext();) {
            IContainer next = iter.next();
            //as files are generated externally, if we don't refresh, it's very likely that we won't delete a bunch of files.
            try {
                next.refreshLocal(IResource.DEPTH_INFINITE, nullProgressMonitor);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
            nDeleted += this.doActionOnContainer(next);
        }
        
        afterRun(nDeleted);
    }


    /**
     * @return true if the action should be run and false otherwise
     */
    protected abstract boolean confirmRun() ;

    
    /**
     * Hook for clients to implement after the run is done (useful to show message)
     * 
     * @param resourcesAffected the number of resources that've been affected.
     */
    protected abstract void afterRun(int resourcesAffected);


    /**
     * Executes the action on the container passed
     * 
     * @param next the container where the action should be executed
     * @return the number of resources affected in the action
     */
    protected abstract int doActionOnContainer(IContainer next);

}
