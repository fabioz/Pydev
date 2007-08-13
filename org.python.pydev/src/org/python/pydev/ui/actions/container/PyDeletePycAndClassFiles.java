package org.python.pydev.ui.actions.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Action used to delete the .pyc and $py.class files (generated from the python or jython interpreter).
 *  
 * @author Fabio
 */
public class PyDeletePycAndClassFiles implements IObjectActionDelegate {
    
    /**
     * List with the containers the user selected 
     */
    protected List<IContainer> selectedContainers;

    
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        //empty
    }

    /**
     * Deletes the files... recursively pass the folders and delete the files (and sum them so that we know how many
     * files were deleted).
     * 
     * @param container the folder from where we want to remove the files
     * @return the number of files deleted
     */
    protected int deletePycFiles(IContainer container) {
        IProgressMonitor nullProgressMonitor = new NullProgressMonitor();
        
        int deleted = 0;
        try{
            IResource[] members = container.members();
            
            for (IResource c:members) {
                if(c instanceof IContainer){
                    deleted += this.deletePycFiles((IContainer) c);
                    
                }else if(c instanceof IFile){
                    String name = c.getName();
                    if(name != null){
                        if(name.endsWith(".pyc") || name.endsWith("$py.class")){
                            c.delete(true, nullProgressMonitor);
                            deleted += 1;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
            
        return deleted;
    }

    /**
     * Act on the selection to delete the pyc/$py.class files.
     */
    public void run(IAction action) {
        //should not happen
        if(selectedContainers == null){
            return;
        }
        
        if (!MessageDialog.openConfirm(null, "Confirm deletion", "Are you sure that you want to delete the *.pyc and *$py.class files from the selected folder(s)?")){
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
            nDeleted += this.deletePycFiles(next);
        }
        
        MessageDialog.openInformation(null, "Files deleted", StringUtils.format("Deleted %s files.", nDeleted));
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

}
