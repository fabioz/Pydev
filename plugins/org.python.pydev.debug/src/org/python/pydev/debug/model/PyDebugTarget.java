/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 23, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.console.ConsoleCompletionsPageParticipant;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;

/**
 * Debugger class that represents a single python process.
 * 
 * It deals with events from RemoteDebugger.
 * Breakpoint updating.
 */
public class PyDebugTarget extends AbstractDebugTarget {
    //private ILaunch launch;
    public volatile IProcess process;
    /** 
     * TODO consider instead of global access to project, have {@link ConsoleCompletionsPageParticipant#init(org.eclipse.ui.part.IPageBookViewPage, org.eclipse.ui.console.IConsole)
     * instead call something like getInterpreterInfo which then PyDebugTargetConsole (which isn't connected to a project)
     * has some hope of resolving 
     */
    public final IProject project;
    public volatile boolean finishedInit = false;
    public final boolean isAuxiliaryDebugTarget;

    public PyDebugTarget(ILaunch launch, IProcess process, IPath[] file, AbstractRemoteDebugger debugger,
            IProject project) {
        this(launch, process, file, debugger, project, false);
    }

    public PyDebugTarget(ILaunch launch, IProcess process, IPath[] file, AbstractRemoteDebugger debugger,
            IProject project, boolean isAuxiliaryDebugTarget) {
        this.launch = launch;
        this.process = process;
        this.file = file;
        this.debugger = debugger;
        this.threads = new PyThread[0];
        this.project = project;
        this.isAuxiliaryDebugTarget = isAuxiliaryDebugTarget;
        launch.addDebugTarget(this);
        debugger.addTarget(this);
        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        breakpointManager.addBreakpointListener(this);
        PyExceptionBreakPointManager.getInstance().addListener(this);
        PyPropertyTraceManager.getInstance().addListener(this);
        // we have to know when we get removed, so that we can shut off the debugger
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        // shut down the remote debugger when parent launch
        if (launch == this.launch) {
            IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.removeBreakpointListener(this);
            PyExceptionBreakPointManager.getInstance().removeListener(this);
            PyPropertyTraceManager.getInstance().removeListener(this);
            debugger.dispose();
            debugger = null;
        }
    }

    @Override
    public IProcess getProcess() {
        return process;
    }

    @Override
    public boolean canTerminate() {
        if (!finishedInit) {
            //We must finish init to terminate
            return false;
        }

        // We can always terminate if it's still not terminated.
        return !this.isTerminated();
    }

    @Override
    public boolean isTerminated() {
        if (!finishedInit) {
            //We must finish init to terminate
            return false;
        }
        if (process == null) {
            return true;
        }
        return process.isTerminated();
    }

    @Override
    public void terminate() {
        if (process != null) {
            if (!this.isAuxiliaryDebugTarget) {
                try {
                    // We can only terminate the process if it's not an auxiliary debug target
                    // (otherwise, when connecting on multiple processes, we may end up terminating
                    // the main process when the child process exits).
                    process.terminate();
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
            process = null;
        }
        super.terminate();
    }

}
