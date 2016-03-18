/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.debug.model.PyPropertyTraceManager;
import org.python.pydev.debug.model.PyThread;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class PyDebugTargetServer extends AbstractDebugTarget {

    private boolean isTerminated;

    public PyDebugTargetServer(ILaunch launch, IPath[] file, RemoteDebuggerServer debugger) {
        this.file = file;
        this.debugger = debugger;
        this.threads = new PyThread[0];
        this.launch = launch;

        if (launch != null) {
            for (IDebugTarget target : launch.getDebugTargets()) {
                if (target instanceof PyDebugTargetServer && target.isTerminated()) {
                    launch.removeDebugTarget(target);
                }
            }
            launch.addDebugTarget(this);
        }

        debugger.addTarget(this);
        PyExceptionBreakPointManager.getInstance().addListener(this);
        PyPropertyTraceManager.getInstance().addListener(this);

        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        breakpointManager.addBreakpointListener(this);
        // we have to know when we get removed, so that we can shut off the debugger
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
    }

    @Override
    public boolean canTerminate() {
        return !isTerminated;
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }

    @Override
    public void terminate() {
        isTerminated = true;
        super.terminate();
    }

    public void setTerminated() {
        isTerminated = true;
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        // shut down the remote debugger when parent launch
        if (launch == this.launch) {
            IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.removeBreakpointListener(this);
            PyExceptionBreakPointManager.getInstance().removeListener(this);
            PyPropertyTraceManager.getInstance().removeListener(this);
        }
    }

    @Override
    public void processCommand(String sCmdCode, String sSeqCode, String payload) {
        if (Integer.parseInt(sCmdCode) == AbstractDebuggerCommand.CMD_WRITE_TO_CONSOLE) {
            ProcessServer serverProcess = getDebugger().getServerProcess();

            //payload = <xml><io s="%s" ctx="%s"/></xml>
            Tuple<String, Integer> message = XMLMessage.getMessage(payload);
            if (message.o2 == 1) {
                serverProcess.writeToStdOut(message.o1);
            } else {
                serverProcess.writeToStdErr(message.o1);
            }
        } else {
            super.processCommand(sCmdCode, sSeqCode, payload);
        }
    }

    @Override
    public RemoteDebuggerServer getDebugger() {
        return (RemoteDebuggerServer) super.getDebugger();
    }

    @Override
    public IProcess getProcess() {
        return getDebugger().getIProcess();
    }

}
