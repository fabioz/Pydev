/*
 * Created on Oct 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pychecker;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;

/**
 * @author Fabio Zadrozny
 */
public class PyCheckerVisitor extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public boolean visitResource(IResource resource, IDocument document) {
        if (PyCheckerPrefPage.usePyChecker() == false) {
            return false;
        }

//        PyCheckerLauncher.pycheck(resource);
        return true;
    }
    
}
