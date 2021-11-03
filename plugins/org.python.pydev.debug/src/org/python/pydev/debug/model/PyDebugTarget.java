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

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.console.ConsoleCompletionsPageParticipant;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.shared_core.structure.Tuple;

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
        super(file);
        this.launch = launch;
        this.process = process;
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
    public void processCommand(String sCmdCode, String sSeqCode, String payload) {
        if (Integer.parseInt(sCmdCode) == AbstractDebuggerCommand.CMD_WRITE_TO_CONSOLE) {
            IConsole console = DebugUITools.getConsole(this.getProcess());
            if (console instanceof org.eclipse.debug.ui.console.IConsole) {
                //payload = <xml><io s="%s" ctx="%s"/></xml>
                try {
                    org.eclipse.debug.ui.console.IConsole processConsole = (org.eclipse.debug.ui.console.IConsole) console;
                    Tuple<String, Integer> message = XMLMessage.getMessage(payload);
                    if (!message.o1.isEmpty()) {
                        IOConsoleOutputStream stream;
                        if (message.o2 == 1) {
                            stream = processConsole.getStream(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
                        } else {
                            stream = processConsole.getStream(IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
                        }
                        if (stream != null) {
                            stream.write(message.o1);
                        } else {
                            Log.log("Unable to find stream for context: " + message.o2);
                        }
                    }
                } catch (IOException e) {
                    Log.log(e);
                }
            }
        } else {
            super.processCommand(sCmdCode, sSeqCode, payload);
        }
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
