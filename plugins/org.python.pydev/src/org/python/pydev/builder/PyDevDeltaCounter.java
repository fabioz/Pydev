/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class PyDevDeltaCounter extends PydevInternalResourceDeltaVisitor {

    private int nVisited = 0;

    public PyDevDeltaCounter() {
        super(null, 0);
    }

    @Override
    protected void handleAddedPycFiles(IResource resource, PythonNature nature) {
        //don't do anything special on pyc files!
    }

    /**
     * Overridden so that we don't load the document on this visitor (there is no need for that).
     */
    @Override
    protected boolean chooseVisit(IResourceDelta delta, IResource resource, boolean isAddOrChange) {
        switch (delta.getKind()) {
            case IResourceDelta.ADDED:
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
    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        nVisited += 1;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
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
