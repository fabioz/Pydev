/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.builder.pycremover.PycHandlerBuilderVisitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;

public abstract class PydevInternalResourceDeltaVisitor extends PyDevBuilderVisitor implements IResourceDeltaVisitor {

    /**
     * this monitor might be set externally so that we can communicate the progress to the user
     * (set externally)
     */
    public IProgressMonitor monitor;

    protected PydevInternalResourceDeltaVisitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Visits the resource delta tree determining which files to rebuild (*.py).
     *
     * Subclasses should only reimplement visitChanged, visitAdded and visitRemoved. This method will not be called
     * in the structure provided by pydev.
     *
     * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException {
        if (delta == null) {
            return true;
        }

        final IResource resource = delta.getResource();

        if (resource == null) {
            return true;
        }

        int type = resource.getType();

        //related bug https://sourceforge.net/tracker/index.php?func=detail&aid=1238850&group_id=85796&atid=577329

        //the team-support plugins of eclipse use the IResource
        //method setTeamPrivateMember to indicate resources
        //that are only in the project for the team-stuff (e.g. .svn or
        //.cvs or _darcs directories).
        if (resource.isTeamPrivateMember()) {
            return true;
        }

        if (type == IResource.FOLDER) {
            switch (delta.getKind()) {
                case IResourceDelta.REMOVED:
                    memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, System.currentTimeMillis());
                    visitRemovedResource(resource, null, monitor);
                    break;
            //for folders, we don't have to do anything if added or changed (we just treat their children, that should
            //resolve for modules -- we do, however have to treat __init__.py differently).
            }

        } else if (type == IResource.FILE) {
            String ext = resource.getFileExtension();
            if (ext == null) { //resource.getFileExtension() may return null if it has none.
                if (resource instanceof IFile) {
                    PythonPathHelper.markAsPyDevFileIfDetected((IFile) resource);
                }
                return true;
            }

            //only analyze projects with the python nature...
            IProject project = resource.getProject();
            PythonNature nature = PythonNature.getPythonNature(project);

            if (project != null && nature != null) {
                //we just want to make the visit if it is a valid python file and it is in the pythonpath
                if (PythonPathHelper.isValidSourceFile("." + ext)) {
                    onVisitDelta(delta);

                } else if (ext.equals("pyc")) {
                    if (delta.getKind() == IResourceDelta.ADDED) {
                        handleAddedPycFiles(resource, nature);
                    }
                }
            }
        }

        return true;
    }

    /**
     * When handling pyc files, we do a simpler handling and go directly to the pyc builder visitor to check
     * if the pyc should be removed.
     */
    protected void handleAddedPycFiles(IResource resource, PythonNature nature) {
        try {
            //we do that because we may have an added .pyc without a correspondent .py file
            PycHandlerBuilderVisitor pycVisitor = new PycHandlerBuilderVisitor();
            pycVisitor.visitingWillStart(monitor, false, nature);
            try {
                pycVisitor.visitAddedResource(resource, null, monitor); //doc is not used in pyc remover
            } finally {
                pycVisitor.visitingEnded(monitor);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    protected abstract void onVisitDelta(IResourceDelta delta);

}
