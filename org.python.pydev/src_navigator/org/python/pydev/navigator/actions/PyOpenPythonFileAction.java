/*
 * Created on Oct 17, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.navigator.PythonNode;

public class PyOpenPythonFileAction extends Action {
    
    private List<IFile> selected;

    private ISelectionProvider provider;

    public PyOpenPythonFileAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        this.setText("Open python file");
        this.provider = selectionProvider;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
        selected = new ArrayList<IFile>();
        
        ISelection selection = provider.getSelection();
        if (!selection.isEmpty()) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            if (sSelection.size() >= 1) {
                Iterator iterator = sSelection.iterator();
                while(iterator.hasNext()){
                    Object element = iterator.next();
                    if(element instanceof PythonNode){
                    	return false;
                    }
                    if(element instanceof IAdaptable){
                        IAdaptable adaptable = (IAdaptable) element;
                        IFile file = (IFile) adaptable.getAdapter(IFile.class);
                        if(file != null){
                            selected.add(file);
                            continue;
                        }
                    }
                    //one of the elements did not satisfy the condition
                    selected = null;
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        if (isEnabled()) {
            for(IFile f:selected){
                new PyOpenAction().run(new ItemPointer(f));
            }
        }
    }

}
