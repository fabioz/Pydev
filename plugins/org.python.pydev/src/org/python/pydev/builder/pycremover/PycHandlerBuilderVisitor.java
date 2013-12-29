/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 14/09/2005
 */
package org.python.pydev.builder.pycremover;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

public class PycHandlerBuilderVisitor extends PyDevBuilderVisitor {

    /**
     * Job that actually deletes the files.
     */
    private static final class PycDeleteJob extends WorkspaceJob {
        public PycDeleteJob() {
            super("Delete .pyc/$py.class files");

        }

        private final List<IFile> files = new ArrayList<IFile>();

        private final Object lock = new Object();

        private void addFilesToDelete(IFile[] files) {
            synchronized (lock) {
                for (IFile f : files) {
                    this.files.add(f);
                }
            }
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            IFile[] currentFilesToDelete = null;
            synchronized (lock) {
                currentFilesToDelete = files.toArray(new IFile[files.size()]);
                files.clear();
            }
            monitor.beginTask("Delete .pyc/$py.class files", currentFilesToDelete.length);
            try {

                for (final IFile workspaceFile : currentFilesToDelete) {
                    if (workspaceFile != null && workspaceFile.exists()) {
                        try {
                            workspaceFile.delete(true, monitor);
                        } catch (CoreException e) {
                            Log.log(e);
                        }
                    }
                    monitor.worked(1);
                }
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }

    }

    private static final PycDeleteJob pycDeleteJob = new PycDeleteJob();

    private static final PySourceLocatorBase locator = new PySourceLocatorBase();

    private int pycDeleteHandling;

    @Override
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {
        super.visitingWillStart(monitor, isFullBuild, nature);
        pycDeleteHandling = PyDevBuilderPrefPage.getPycDeleteHandling();
    }

    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        //Ignore: for pyc files we only care about their addition.
    }

    /**
     * When a .pyc/$py.class file is found, we remove it if it doesn't have the correspondent .py or .pyw class.
     */
    @Override
    public void visitAddedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        switch (pycDeleteHandling) {
            case PyDevBuilderPrefPage.PYC_NEVER_DELETE:
                //See: never delete!
                return;
            case PyDevBuilderPrefPage.PYC_DELETE_WHEN_PY_IS_DELETED:
                //We just found a pyc (not a remove from a .py), so, don't delete.
                return;
            case PyDevBuilderPrefPage.PYC_ALWAYS_DELETE:
                //keep on going
        }

        final String loc = resource.getLocation().toOSString();
        if (loc != null && (loc.endsWith(".pyc") || loc.endsWith("$py.class"))) {
            String dotPyLoc = null;

            final FastStringBuffer buf = new FastStringBuffer(StringUtils.stripExtension(loc), 8);
            for (String ext : FileTypesPreferencesPage.getDottedValidSourceFiles()) {
                buf.append(ext);
                final String bufStr = buf.toString();
                File file = new File(bufStr);
                if (dotPyLoc == null) {
                    dotPyLoc = bufStr;
                }
                if (file.exists()) {
                    markAsDerived(resource);
                    return;
                }
                buf.deleteLastChars(ext.length());
            }

            //this is needed because this method might be called alone (not in the grouper that checks
            //if it is in the pythonpath before)
            //
            //this happens only when a .pyc file is found... if it was a .py file, this would not be needed (as is the
            //case in the visit removed resource)
            IPythonNature nature = PythonNature.getPythonNature(resource);
            if (nature == null) {
                markAsDerived(resource);
                return;
            }
            try {
                if (!nature.isResourceInPythonpathProjectSources(dotPyLoc, false)) {
                    return; // we only analyze resources that are source folders (not external folders)
                }
            } catch (Exception e) {
                Log.log(e);
                return;
            }

            //if still did not return, let's remove it
            deletePycFile(loc);
        }
    }

    /**
     * We must mark .pyc files as derived.
     * @param resource the resource to be marked as derived.
     */
    private void markAsDerived(IResource resource) {
        try {
            resource.setDerived(true);
        } catch (CoreException e) {
            Log.log(e);
        }
    }

    /**
     * When a .py file is removed (which is what we check for), we go on and remove the .pyc file too.
     */
    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        switch (pycDeleteHandling) {
            case PyDevBuilderPrefPage.PYC_NEVER_DELETE:
                //See: never delete!
                return;
        }

        String loc = resource.getLocation().toOSString();
        if (PythonPathHelper.isValidSourceFile(loc)) {
            String withoutExt = StringUtils.stripExtension(loc);
            deletePycFile(withoutExt + ".pyc");
            deletePycFile(withoutExt + "$py.class");
        }
    }

    /**
     * Deletes .pyc files
     */
    private void deletePycFile(String loc) {
        if (loc.endsWith(".pyc") || loc.endsWith("$py.class")) {
            try {
                File file = new File(loc);

                //remove all: file and links
                final IFile[] files = locator.getWorkspaceFiles(file);

                if (files == null || files.length == 0) {
                    return;
                }

                pycDeleteJob.addFilesToDelete(files);
                pycDeleteJob.schedule(200);

            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

}
