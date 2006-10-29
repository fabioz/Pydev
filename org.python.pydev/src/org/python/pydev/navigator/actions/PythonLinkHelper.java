/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.navigator.IWrappedResource;

public class PythonLinkHelper implements ILinkHelper {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.ILinkHelper#findSelection(org.eclipse.ui.IEditorInput)
     */
    public IStructuredSelection findSelection(IEditorInput anInput) {
        if (anInput instanceof IFileEditorInput){
            return new StructuredSelection(((IFileEditorInput) anInput).getFile());
        }
        return StructuredSelection.EMPTY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.ILinkHelper#activateEditor(org.eclipse.ui.IWorkbenchPage, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
        if (aSelection == null || aSelection.isEmpty()){
            return;
        }
        
        Object firstElement = aSelection.getFirstElement();
        
        //if it is a python element, let's first get the actual object for finding the editor
        if (firstElement instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) firstElement;
            firstElement = resource.getActualObject();
        }
        
        //and now, if it is really a file...
        if (firstElement instanceof IFile) {
            IEditorInput fileInput = new FileEditorInput((IFile) firstElement);
            IEditorPart editor = null;
            if ((editor = aPage.findEditor(fileInput)) != null){
                aPage.bringToTop(editor);
            }
        }

    }

}
