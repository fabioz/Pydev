/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.navigator.elements.IWrappedResource;

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
        if(anInput instanceof IAdaptable){
            //handles org.eclipse.compare.CompareEditorInput without a specific reference to it
            Object adapter = anInput.getAdapter(IFile.class);
            if(adapter != null){
                return new StructuredSelection(adapter);
            }
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
            
            //ok, let's check if the active editor is already the selection, because although the findEditor(editorInput) method
            //may return an editor for the correct file, we may have multiple editors for the same file, and if the current
            //editor is already correct, we don't want to change it
            //@see bug: https://sourceforge.net/tracker/?func=detail&atid=577329&aid=2037682&group_id=85796
            IEditorPart activeEditor = aPage.getActiveEditor();
            if(activeEditor != null){
                IEditorInput editorInput = activeEditor.getEditorInput();
                IFile currFile = (IFile) editorInput.getAdapter(IFile.class);
                if(currFile != null && currFile.equals(firstElement)){
                    return; //the current editor is already the active editor.
                }
            }
            
            //if we got here, the active editor is not a match, so, let's find one and show it.
            IEditorPart editor = null;
            IEditorInput fileInput = new FileEditorInput((IFile) firstElement);
            if ((editor = aPage.findEditor(fileInput)) != null){
                aPage.bringToTop(editor);
            }
        }

    }

}
