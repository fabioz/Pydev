/*
 * Created on Oct 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pychecker;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;

/**
 * @author Fabio Zadrozny
 */
public class PyCheckerVisitor extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        if (PyCheckerPrefPage.usePyChecker() == false) {
            return ;
        }

    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }
    
}
