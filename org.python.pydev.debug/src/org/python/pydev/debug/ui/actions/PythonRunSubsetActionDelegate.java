/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.python.pydev.debug.codecoverage.PyCoverage;
import org.python.pydev.debug.codecoverage.RunManyDialog;

/**
 * @author Fabio Zadrozny
 */
public class PythonRunSubsetActionDelegate extends ActionDelegate implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private IFolder selectedFolder;

	public void run(IAction action) {
		if (part != null && selectedFolder != null) {
			// figure out run or debug mode
			
			RunManyDialog dialog = new RunManyDialog(part.getSite().getShell(), selectedFolder.getLocation().toString());
			if(dialog.open() == Window.OK){
			    String root = dialog.rootFolder;
			    String files = dialog.files;
			    
			    String interpreter = dialog.interpreter;
			    String workingDir = dialog.working;
			    
			    List list = listFilesThatMatch(root, files);
			    for (Iterator iter = list.iterator(); iter.hasNext();) {
                    System.out.println(iter.next());
                    //TODO: execute those files with the coverage script.
                    //we have to get the output for the user..
                }
			}
		}
	}

	/**
     * @param root
     * @param files
     */
    private List listFilesThatMatch(String root, final String filesFilter) {
        List l = new ArrayList();

        File file = new File(root);
        if(file.exists()){
            FileFilter filter = new FileFilter() {

                public boolean accept(File pathname) {
                    return pathname.isDirectory() == false && pathname.getName().matches(filesFilter);
                }

            };

            l = PyCoverage.getPyFilesBelow(file, filter)[0];
        }
        
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;

    }
	public void selectionChanged(IAction action, ISelection selection) {
	    selectedFolder = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedResource = structuredSelection.getFirstElement();
				if (selectedResource instanceof IFolder)
				    selectedFolder = (IFolder) selectedResource;
			}
		}
	}

}