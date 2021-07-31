/**
 * Copyright (c) 2021 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.flake8;

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

/*default*/ public final class Flake8Visitor extends OnlyRemoveMarkersFlake8Visitor {

    private IDocument document;
    private IProgressMonitor monitor;
    private final Object lock = new Object();

    /*default*/ Flake8Visitor(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor monitor) {
        super(resource);
        this.document = document;
        this.monitor = monitor;
    }

    private Flake8Analysis flake8Runnable;

    /**
     * When we start visiting some resource, we create the process which will do the Flake8 analysis.
     */
    @Override
    public void startVisit() {
        if (resource == null || Flake8Preferences.useFlake8(resource) == false
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

        File flake8Location = Flake8Preferences.getFlake8Location(pythonNature);
        if (flake8Location == null || !FileUtils.enhancedIsFile(flake8Location)) {
            if (flake8Location == null) {
                Log.log("Unable to find flake8. Project: " + project.getName());
            } else {
                Log.log("flake8 location does not exist: " + flake8Location);
            }
            deleteMarkers();
            return;
        }

        try {
            // Flake8 can only be used for python projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                deleteMarkers();
                return;
            }
        } catch (Exception e) {
            deleteMarkers();
            return;
        }
        synchronized (lock) {
            if (flake8Runnable != null) {
                // If the flake8Runnable is already created, don't recreate it
                // (we should be analyzing multiple resources in a single call).
                return;
            }

            if (project != null) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    IPath location = file.getLocation();
                    if (location != null) {
                        flake8Runnable = new Flake8Analysis(resource, document, location,
                                new NullProgressMonitorWrapper(monitor), flake8Location);

                        try {
                            IExternalCodeAnalysisStream out = Flake8Preferences.getConsoleOutputStream(project);
                            flake8Runnable.createFlake8Process(out);
                        } catch (final Exception e) {
                            Log.log(e);
                        }
                    }
                } else if (resource instanceof IContainer) {
                    IContainer dir = (IContainer) resource;
                    IPath location = dir.getLocation();
                    if (location != null) {
                        flake8Runnable = new Flake8Analysis(resource, null, location,
                                new NullProgressMonitorWrapper(monitor), flake8Location);

                        try {
                            IExternalCodeAnalysisStream out = Flake8Preferences.getConsoleOutputStream(project);
                            flake8Runnable.createFlake8Process(out);
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
        if (flake8Runnable != null) {
            flake8Runnable.join();
        }
    }

    @Override
    public List<PyMarkerUtils.MarkerInfo> getMarkers(IResource resource) {
        List<PyMarkerUtils.MarkerInfo> ret = new ArrayList<PyMarkerUtils.MarkerInfo>();
        if (flake8Runnable == null) {
            return ret;
        }
        return flake8Runnable.getMarkers(resource);
    }

    @Override
    public boolean getRequiresAnalysis() {
        return true;
    }

}
