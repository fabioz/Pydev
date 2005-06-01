/*
 * Created on Jan 31, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public class PyDevDeltaCounter extends PyDevBuilderVisitor{

    private int nVisited = 0;
    
    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitChangedResource(IResource resource, IDocument document) {
        nVisited += 1;
        return true;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return true;
    }

    /**
     * @param nVisited The nVisited to set.
     */
    public void setNVisited(int nVisited) {
        this.nVisited = nVisited;
    }

    /**
     * @return Returns the nVisited.
     */
    public int getNVisited() {
        return nVisited;
    }


}
