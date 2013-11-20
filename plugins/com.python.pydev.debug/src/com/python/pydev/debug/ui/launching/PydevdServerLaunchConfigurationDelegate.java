/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.ui.launching;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchConfigurationDelegate;

import com.python.pydev.debug.model.ProcessServer;
import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class PydevdServerLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate {

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

        monitor.beginTask("Preparing configuration", 1);
        monitor.worked(1);

        ProcessServer p = new ProcessServer();
        String label = "Debug Server";
        HashMap<String, String> processAttributes = new HashMap<String, String>();
        processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
        processAttributes.put(IProcess.ATTR_PROCESS_LABEL, label);
        processAttributes.put(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");

        IProcess pro = DebugPlugin.newProcess(launch, p, label, processAttributes);

        RemoteDebuggerServer.getInstance().setLaunch(launch, p, pro);
    }

    @Override
    protected String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch) {
        return "RUN_SERVER";
    }
}
