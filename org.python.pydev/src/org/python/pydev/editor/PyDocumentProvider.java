/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */
 
package org.python.pydev.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * DocumentProvider creates and manages the document content. 
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
		return document;
	}

	private IDocumentPartitioner createDocumentPartitioner() {
		IDocumentPartitioner partitioner =
			new DefaultPartitioner(
				new PyPartitionScanner(), PyPartitionScanner.getTypes());
		return partitioner;
	}
}