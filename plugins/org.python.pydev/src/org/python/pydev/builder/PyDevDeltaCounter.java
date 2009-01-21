/*
 * Created on Jan 31, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public class PyDevDeltaCounter extends PydevInternalResourceDeltaVisitor{

    private int nVisited = 0;

    public PyDevDeltaCounter(){
        super(null, 0);
    }
    
    /**
     * Overriden so that we don't load the document on this visitor (there is no need for that).
     */
    protected boolean chooseVisit(IResourceDelta delta, IResource resource, boolean isAddOrChange) {
        switch (delta.getKind()) {
            case IResourceDelta.ADDED :
                visitAddedResource(resource, null, monitor);
                isAddOrChange = true;
                break;
            case IResourceDelta.CHANGED:
                visitChangedResource(resource, null, monitor);
                isAddOrChange = true;
                break;
            case IResourceDelta.REMOVED:
                visitRemovedResource(resource, null, monitor);
                break;
        }
        return isAddOrChange;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        nVisited += 1;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
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
