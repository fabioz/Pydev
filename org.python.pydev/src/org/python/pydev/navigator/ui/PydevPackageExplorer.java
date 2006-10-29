/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

public class PydevPackageExplorer extends CommonNavigator implements IShowInTarget{
    /**
     * Returns the element contained in the EditorInput
     */
    Object getElementOfInput(IEditorInput input) {
        if (input instanceof IFileEditorInput){
            return ((IFileEditorInput)input).getFile();
        }
        return null;
    }

    public boolean show(ShowInContext context) {
        ISelection selection= context.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection= ((IStructuredSelection) selection);
            if (structuredSelection.size() == 1 && tryToReveal(structuredSelection.getFirstElement()))
                return true;
        }
        
        Object input= context.getInput();
        if (input instanceof IEditorInput) {
            Object elementOfInput= getElementOfInput((IEditorInput)context.getInput());
            return elementOfInput != null && tryToReveal(elementOfInput);
        }

        return false;
    }
    
    public boolean tryToReveal(Object element) {
        if (revealElementOrParent(element))
            return true;
        return false;
    }
    
    private boolean revealElementOrParent(Object element) {
        if (revealAndVerify(element))
            return true;
        if (element != null) {
            if (revealAndVerify(element))
                return true;
            if (element instanceof IJavaElement) {
                IResource resource= ((IJavaElement)element).getResource();
                if (resource != null) {
                    if (revealAndVerify(resource))
                        return true;
                }
            }
        }
        return false;
    }
    
    
    private boolean revealAndVerify(Object element) {
        if (element == null)
            return false;
        selectReveal(new StructuredSelection(element));
        return ! getSite().getSelectionProvider().getSelection().isEmpty();
    }



}
