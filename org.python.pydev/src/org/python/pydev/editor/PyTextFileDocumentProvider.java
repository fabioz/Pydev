/*
 * License: Common Public License v1.0
 * Created on Jun 24, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class PyTextFileDocumentProvider extends TextFileDocumentProvider {

    private DefaultPartitioner partitioner= new DefaultPartitioner(new PyPartitionScanner(), 
			  PyPartitionScanner.getTypes());

    /**
     *  
     */
    public PyTextFileDocumentProvider() {
        super(new PyDocumentProvider());
    }

    /**
     * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#getDocument(java.lang.Object)
     */
    public IDocument getDocument(Object element) {
        IDocument document = super.getDocument(element);
        if (document != null) {
            IDocumentPartitioner partitioner2 = document.getDocumentPartitioner();
            if(partitioner2 != partitioner){
                partitioner.connect(document);
                document.setDocumentPartitioner(partitioner);
            }
        }
		// Also adds Python nature to the project.
		// The reason this is done here is because I want to assign python
		// nature automatically to any project that has active python files.
		if (element instanceof FileEditorInput) {
			IFile file = (IFile)((FileEditorInput)element).getAdapter(IFile.class);
			if (file != null){
				try {
                    PythonNature.addNature(file.getProject(), null);
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
			}
		}

        return document;
    }

}
