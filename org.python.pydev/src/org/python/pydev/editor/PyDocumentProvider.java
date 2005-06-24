/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
 
package org.python.pydev.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.part.FileEditorInput;

/**
 * DocumentProvider creates and manages the document content. 
 * 
 * It notifies the editors about changes applied to the document model. 
 * The document provider also creates an annotation model
 * on a document. 
 * A document is an abstraction, it is not limited to representing 
 * text files. However, 
 * FileDocumentProvider connects to resource based (IFile) documents
 */

public class PyDocumentProvider extends FileDocumentProvider {

	/**
	 * Implementation of Open External File functionality
	 */
	protected boolean setDocumentContent(IDocument document,
			IEditorInput editorInput, String encoding) throws CoreException {
		boolean retVal = super.setDocumentContent(document, editorInput, encoding);
		// Dealing with external files
		if (retVal == false) {
			// for Open External File, we get JavaFile, which we have no access to
			// luckily, this object is also a ILocationProvider

			if (editorInput instanceof ILocationProvider) {
				IPath path = ((ILocationProvider)editorInput).getPath(editorInput);
				path = path.makeAbsolute();
		        IWorkspace ws = ResourcesPlugin.getWorkspace();
		        IProject project = ws.getRoot().getProject("External Files");
		        if (!project.exists())
		           project.create(null);
		        if (!project.isOpen())
		           project.open(null);
	
		        IFile file = project.getFile(path.lastSegment());
		        try{
		            file.createLink(path, IResource.NONE, null);
		        }catch (Exception e) {
                }
                return super.setDocumentContent(document, new FileEditorInput(file), encoding);
			}
		    
		}
		return true;
	}
}
