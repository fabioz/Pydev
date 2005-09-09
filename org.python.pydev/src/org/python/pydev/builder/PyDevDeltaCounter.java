/*
 * Created on Jan 31, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public class PyDevDeltaCounter extends PyDevBuilderVisitor{

    private int nVisited = 0;
    public ArrayList<IResource> changed;
    public ArrayList<IResource> removed;

    public PyDevDeltaCounter(){
        changed = new ArrayList<IResource>();
        removed = new ArrayList<IResource>();
    }
    
    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitChangedResource(IResource resource, IDocument document) {
        nVisited += 1;
        changed.add(resource);
        return true;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        removed.add(resource);
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
