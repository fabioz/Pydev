/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ruff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
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
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.progress.NullProgressMonitorWrapper;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

/**
 * @author Fabio Zadrozny
 */
/*default*/ public final class RuffVisitor extends OnlyRemoveMarkersRuffVisitor {

    private IDocument document;
    private IProgressMonitor monitor;
    private final Object lock = new Object();

    /*default*/ RuffVisitor(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor monitor) {
        super(resource);
        this.document = document;
        this.monitor = monitor;
    }

    private RuffAnalysis ruffRunnable;

    /**
     * When we start visiting some resource, we create the process which will do the Ruff analysis.
     */
    @Override
    public void startVisit() {
        if (resource == null || RuffPreferences.useRuff(resource) == false
                || (document == null && !(resource instanceof IContainer))) {
            deleteMarkers();
            return;
        }

        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        if (pythonNature == null) {
            deleteMarkers();
            return;
        }

        // null is expected when we should do 'python -m ruff ...'.
        File ruffLocation = null;
        if (!RuffPreferences.useRuffFromPythonNature(pythonNature, project)) {
            ruffLocation = RuffPreferences.getRuffLocation(pythonNature);
            if (ruffLocation == null || !FileUtils.enhancedIsFile(ruffLocation)) {
                if (ruffLocation == null) {
                    Log.log("Unable to find ruff. Project: " + project.getName());
                } else {
                    Log.log("ruff location does not exist: " + ruffLocation);
                }
                deleteMarkers();
                return;
            }
        }

        try {
            // Ruff can only be used for python projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                deleteMarkers();
                return;
            }
        } catch (Exception e) {
            deleteMarkers();
            return;
        }
        synchronized (lock) {
            if (ruffRunnable != null) {
                // If the ruffRunnable is already created, don't recreate it
                // (we should be analyzing multiple resources in a single call).
                return;
            }

            if (project != null) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    IPath location = file.getLocation();
                    if (location != null) {
                        ruffRunnable = new RuffAnalysis(resource, document, location,
                                new NullProgressMonitorWrapper(monitor), ruffLocation);

                        try {
                            IExternalCodeAnalysisStream out = RuffPreferences.getConsoleOutputStream(project);
                            ruffRunnable.createRuffProcess(out);
                        } catch (final Exception e) {
                            Log.log(e);
                        }
                    }
                } else if (resource instanceof IContainer) {
                    IContainer dir = (IContainer) resource;
                    IPath location = dir.getLocation();
                    if (location != null) {
                        ruffRunnable = new RuffAnalysis(resource, null, location,
                                new NullProgressMonitorWrapper(monitor), ruffLocation);

                        try {
                            IExternalCodeAnalysisStream out = RuffPreferences.getConsoleOutputStream(project);
                            ruffRunnable.createRuffProcess(out);
                        } catch (final Exception e) {
                            Log.log(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void join() {
        if (ruffRunnable != null) {
            ruffRunnable.join();
        }
    }

    @Override
    public List<PyMarkerUtils.MarkerInfo> getMarkers(IResource resource) {
        List<PyMarkerUtils.MarkerInfo> ret = new ArrayList<PyMarkerUtils.MarkerInfo>();
        if (ruffRunnable == null) {
            return ret;
        }
        return ruffRunnable.getMarkers(resource);
    }

    @Override
    public boolean getRequiresAnalysis() {
        return true;
    }

}
