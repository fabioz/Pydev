/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Aug 16, 2003
 */
package org.python.pydev.debug.ui.launching;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.EditorUtils;


/**
 * 
 * Launcher for the python scripts.
 * 
 * <p>The code is pretty much copied from ExternalTools' ProgramLaunchDelegate.
 * <p>I would have subclassed, but ProgramLaunchDelegate hides important internals
 * 
 * Based on org.eclipse.ui.externaltools.internal.program.launchConfigurations.ProgramLaunchDelegate
 * 
 * Build order based on org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
 */
public abstract class AbstractLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements
        ILaunchConfigurationDelegate {

    private IProject[] fOrderedProjects;

    /**
     * We need to reimplement this method (otherwise, all the projects in the workspace will be rebuilt, and not only
     * the ones referenced in the configuration).
     */
    @Override
    protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
        return fOrderedProjects;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration,
     *      java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
            throws CoreException {
        // build project list
        fOrderedProjects = null;

        String projName = configuration.getAttribute(Constants.ATTR_PROJECT, "");
        if (projName.length() > 0) {

            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);

            if (project != null) {
                fOrderedProjects = computeReferencedBuildOrder(new IProject[] { project });
            }
        }

        // do generic launch checks
        return super.preLaunchCheck(configuration, mode, monitor);
    }

    /**
     * Launches the python process.
     * 
     * Modelled after Ant & Java runners
     * see WorkbenchLaunchConfigurationDelegate::launch
     */
    @Override
    public void launch(ILaunchConfiguration conf, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        monitor.beginTask("Preparing configuration", 3);

        try {
            PythonRunnerConfig runConfig = new PythonRunnerConfig(conf, mode, getRunnerConfigRun(conf, mode, launch));

            monitor.worked(1);
            try {
                PythonRunner.run(runConfig, launch, monitor);
            } catch (IOException e) {
                Log.log(e);
                finishLaunchWithError(launch);
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                        "Unexpected IO Exception in Pydev debugger", null));
            }
        } catch (final InvalidRunException e) {
            handleError(launch, e);
        } catch (final MisconfigurationException e) {
            handleError(launch, e);
        }
    }

    private void handleError(ILaunch launch, final Exception e) {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                ErrorDialog.openError(EditorUtils.getShell(), "Invalid launch configuration",
                        "Unable to make launch because launch configuration is not valid",
                        PydevPlugin.makeStatus(IStatus.ERROR, e.getMessage(), e));
            }
        });
        finishLaunchWithError(launch);
    }

    private void finishLaunchWithError(ILaunch launch) {
        try {
            launch.terminate();

            ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            launchManager.removeLaunch(launch);
        } catch (Throwable x) {
            Log.log(x);
        }
    }

    /**
     * @return the mode we should use to run it...
     * 
     * @see PythonRunnerConfig#RUN_REGULAR
     * @see PythonRunnerConfig#RUN_COVERAGE
     * @see PythonRunnerConfig#RUN_UNITTEST
     * @see PythonRunnerConfig#RUN_JYTHON_UNITTEST
     * @see PythonRunnerConfig#RUN_JYTHON
     * @see PythonRunnerConfig#RUN_IRONPYTHON
     * @see PythonRunnerConfig#RUN_IRONPYTHON_UNITTEST
     */
    protected abstract String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch);
}
