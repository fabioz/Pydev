/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.pylint;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.progress.NullProgressMonitorWrapper;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

/**
 * Check lint.py for options.
 *
 * @author Fabio Zadrozny
 */
/*default*/ public final class PyLintVisitor extends OnlyRemoveMarkersPyLintVisitor {

    private IDocument document;
    private IProgressMonitor monitor;

    /*default*/ PyLintVisitor(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor monitor) {
        super(resource);
        this.document = document;
        this.monitor = monitor;
    }

    private PyLintAnalysis pyLintRunnable;

    /**
     * When we start visiting some resource, we create the process which will do the PyLint analysis.
     */
    @Override
    public void startVisit() {
        if (document == null || resource == null || PyLintPreferences.usePyLint() == false) {
            deleteMarkers();
            return;
        }

        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        if (pythonNature == null) {
            deleteMarkers();
            return;
        }

        File pyLintLocation = PyLintPreferences.getPyLintLocation(pythonNature);
        if (pyLintLocation == null || !pyLintLocation.exists()) {
            deleteMarkers();
            return;
        }

        try {
            // PyLint can only be used for python projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                deleteMarkers();
                return;
            }
        } catch (Exception e) {
            deleteMarkers();
            return;
        }
        if (project != null && resource instanceof IFile) {
            IFile file = (IFile) resource;
            IPath location = file.getRawLocation();
            if (location != null) {
                pyLintRunnable = new PyLintAnalysis(resource, document, location,
                        new NullProgressMonitorWrapper(monitor), pyLintLocation);

                try {
                    IExternalCodeAnalysisStream out = PyLintPreferences.getConsoleOutputStream();
                    pyLintRunnable.createPyLintProcess(out);
                } catch (final Exception e) {
                    Log.log(e);
                }
            }
        }
    }

    @Override
    public void join() {
        if (pyLintRunnable != null) {
            pyLintRunnable.join();
        }
    }

    @Override
    public List<PyMarkerUtils.MarkerInfo> getMarkers() {
        if (pyLintRunnable == null) {
            return null;
        }
        return pyLintRunnable.markers;
    }

}
