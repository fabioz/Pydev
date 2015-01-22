/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * @author Fabio Zadrozny
 */
public final class PyDevDeltaCounter extends PydevInternalResourceDeltaVisitor {

    private int nVisited = 0;

    public PyDevDeltaCounter() {
        super(null);
    }

    @Override
    protected void handleAddedPycFiles(IResource resource, PythonNature nature) {
        //don't do anything special on pyc files!
    }

    /**
     * Overridden so that we don't load the document on this visitor (there is no need for that).
     */
    @Override
    protected void onVisitDelta(IResourceDelta delta) {
        switch (delta.getKind()) {
            case IResourceDelta.ADDED:
                visitAddedResource(null, null, monitor);
                break;
            case IResourceDelta.CHANGED:
                visitChangedResource(null, null, monitor);
                break;
            case IResourceDelta.REMOVED:
                visitRemovedResource(null, null, monitor);
                break;
        }
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
