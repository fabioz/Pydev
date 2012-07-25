/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Groups the visitors to be added and visits them according to their priority
 * 
 * @author fabioz
 */
public class PydevGrouperVisitor extends PydevInternalResourceDeltaVisitor {

    private List<PyDevBuilderVisitor> visitors;

    public PydevGrouperVisitor(List<PyDevBuilderVisitor> _visitors, IProgressMonitor monitor, int totalResources) {
        super(monitor, totalResources);
        //make a copy - should be already sorted at this point
        this.visitors = new ArrayList<PyDevBuilderVisitor>(_visitors);
    }

    private final int VISIT_ADD = 1;
    private final int VISIT_CHANGE = 2;
    private final int VISIT_REMOVE = 3;

    /**
     * @param name determines the name of the method to visit (added removed or changed)
     * @param resource the resource to visit
     * @param document the document from the resource
     * @param monitor 
     */
    private void visitWith(int visitType, final IResource resource, ICallback0<IDocument> document,
            IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return; //it's already cancelled
        }
        IPythonNature nature = PythonNature.getPythonNature(resource);
        if (nature == null) {
            return;
        }
        if (!nature.startRequests()) {
            return;
        }

        try {

            try {
                //we visit external because we must index them
                if (!isResourceInPythonpathProjectSources(resource, nature, true)) {
                    return; // we only analyze resources that are in the pythonpath
                }
            } catch (Exception e1) {
                Log.log(e1);
                return; // we only analyze resources that are in the pythonpath
            }

            HashMap<String, Object> copyMemo = new HashMap<String, Object>(this.memo);
            FastStringBuffer bufferToCommunicateProgress = new FastStringBuffer();

            for (PyDevBuilderVisitor visitor : visitors) {
                // some visitors cannot visit too many elements because they do a lot of processing
                if (visitor.maxResourcesToVisit() == PyDevBuilderVisitor.MAX_TO_VISIT_INFINITE
                        || visitor.maxResourcesToVisit() >= totalResources) {
                    visitor.memo = copyMemo; //setting the memo must be the first thing.
                    try {
                        //communicate progress for each visitor
                        PyDevBuilder.communicateProgress(monitor, totalResources, currentResourcesVisited, resource,
                                visitor, bufferToCommunicateProgress);
                        switch (visitType) {
                            case VISIT_ADD:
                                visitor.visitAddedResource(resource, document, monitor);
                                break;

                            case VISIT_CHANGE:
                                visitor.visitChangedResource(resource, document, monitor);
                                break;

                            case VISIT_REMOVE:
                                visitor.visitRemovedResource(resource, document, monitor);
                                break;

                            default:
                                throw new RuntimeException("Error: visit type not properly given!"); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }

        } finally {
            nature.endRequests();
        }

    }

    @Override
    public void visitAddedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        visitWith(VISIT_ADD, resource, document, monitor);
    }

    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        visitWith(VISIT_CHANGE, resource, document, monitor);
    }

    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        visitWith(VISIT_REMOVE, resource, document, monitor);
    }

}
