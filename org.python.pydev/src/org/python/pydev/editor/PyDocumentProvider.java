/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
 
package org.python.pydev.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.plugin.PythonNature;

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
	 * template code for connecting partitioner & document
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner = createDocumentPartitioner();
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}

		// Also adds Python nature to the project.
		// The reason this is done here is because I want to assign python
		// nature automatically to any project that has active python files.
		if (element instanceof FileEditorInput) {
			IFile file = (IFile)((FileEditorInput)element).getAdapter(IFile.class);
			if (file != null)
				PythonNature.addNature(file.getProject(), null);
		}
		return document;
	}

	private IDocumentPartitioner createDocumentPartitioner() {
		IDocumentPartitioner partitioner =
			new DefaultPartitioner(
				new PyPartitionScanner(), PyPartitionScanner.getTypes());
		return partitioner;
	}
	
	
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
				File f = path.toFile();
				try {
					setDocumentContent(document, new FileInputStream(f), encoding);
				} catch (FileNotFoundException e) {
					return false;
				}
				return true;
			}
		}
		return true;
	}
}
