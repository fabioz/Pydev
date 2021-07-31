/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

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
/*default*/ public final class MypyVisitor extends OnlyRemoveMarkersMypyVisitor {

    private IDocument document;
    private IProgressMonitor monitor;
    private final Object lock = new Object();

    /*default*/ MypyVisitor(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor monitor) {
        super(resource);
        this.document = document;
        this.monitor = monitor;
    }

    private MypyAnalysis mypyRunnable;

    /**
     * When we start visiting some resource, we create the process which will do the Mypy analysis.
     */
    @Override
    public void startVisit() {
        if (resource == null || MypyPreferences.useMypy(resource) == false
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

        File mypyLocation = MypyPreferences.getMypyLocation(pythonNature);
        if (mypyLocation == null || !FileUtils.enhancedIsFile(mypyLocation)) {
            if (mypyLocation == null) {
                Log.log("Unable to find mypy. Project: " + project.getName());
            } else {
                Log.log("mypy location does not exist: " + mypyLocation);
            }
            deleteMarkers();
            return;
        }

        try {
            // Mypy can only be used for python projects
            if (pythonNature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                deleteMarkers();
                return;
            }
        } catch (Exception e) {
            deleteMarkers();
            return;
        }
        synchronized (lock) {
            if (mypyRunnable != null) {
                // If the mypyRunnable is already created, don't recreate it
                // (we should be analyzing multiple resources in a single call).
                return;
            }

            if (project != null) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    IPath location = file.getLocation();
                    if (location != null) {
                        mypyRunnable = new MypyAnalysis(resource, document, location,
                                new NullProgressMonitorWrapper(monitor), mypyLocation);

                        try {
                            IExternalCodeAnalysisStream out = MypyPreferences.getConsoleOutputStream(project);
                            mypyRunnable.createMypyProcess(out);
                        } catch (final Exception e) {
                            Log.log(e);
                        }
                    }
                } else if (resource instanceof IContainer) {
                    IContainer dir = (IContainer) resource;
                    IPath location = dir.getLocation();
                    if (location != null) {
                        mypyRunnable = new MypyAnalysis(resource, null, location,
                                new NullProgressMonitorWrapper(monitor), mypyLocation);

                        try {
                            IExternalCodeAnalysisStream out = MypyPreferences.getConsoleOutputStream(project);
                            mypyRunnable.createMypyProcess(out);
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
        if (mypyRunnable != null) {
            mypyRunnable.join();
        }
    }

    @Override
    public List<PyMarkerUtils.MarkerInfo> getMarkers(IResource resource) {
        List<PyMarkerUtils.MarkerInfo> ret = new ArrayList<PyMarkerUtils.MarkerInfo>();
        if (mypyRunnable == null) {
            return ret;
        }
        return mypyRunnable.getMarkers(resource);
    }

    @Override
    public boolean getRequiresAnalysis() {
        return true;
    }

}
