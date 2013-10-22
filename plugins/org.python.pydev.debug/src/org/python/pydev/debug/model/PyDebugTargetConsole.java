/******************************************************************************
* Copyright (C) 2012-2013  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.debug.model.remote.RemoteDebuggerConsole;
import org.python.pydev.debug.newconsole.IPydevConsoleDebugTarget;
import org.python.pydev.debug.newconsole.PydevConsole;
import org.python.pydev.debug.newconsole.PydevConsoleCommunication;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleManager;

/**
 * This is used so that the interactive console can support full debug features.
 */
public class PyDebugTargetConsole extends PyDebugTarget implements IPydevConsoleDebugTarget {

    PyThreadConsole virtualConsoleThread;
    IThread[] virtualConsoleThreads;
    private ScriptConsole console;

    public PyDebugTargetConsole(PydevConsoleCommunication scriptConsoleCommunication, ILaunch launch, IProcess process,
            RemoteDebuggerConsole debugger) {
        super(launch, process, null, debugger, null);

        virtualConsoleThread = new PyThreadConsole(this);
        virtualConsoleThreads = new IThread[] { virtualConsoleThread };
    }

    @Override
    public RemoteDebuggerConsole getDebugger() {
        return (RemoteDebuggerConsole) super.getDebugger();
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        if (isTerminated()) {
            return new IThread[0];
        }
        IThread[] realThreads = super.getThreads();
        if (realThreads != null) {
            return ArrayUtils.concatArrays(virtualConsoleThreads, realThreads);
        } else {
            return virtualConsoleThreads;
        }
    }

    private IStackFrame[] createFrames() {
        PyStackFrameConsole frame = new PyStackFrameConsole(virtualConsoleThread, this);
        return new IStackFrame[] { frame };
    }

    public void setSuspended(boolean suspended) {
        if (suspended != virtualConsoleThread.isSuspended()) {
            final int state;
            if (suspended) {
                state = DebugEvent.SUSPEND;
                virtualConsoleThread.setSuspended(true, createFrames());
            } else {
                state = DebugEvent.RESUME;
                virtualConsoleThread.setSuspended(false, null);
            }
            fireEvent(new DebugEvent(virtualConsoleThread, state, DebugEvent.CLIENT_REQUEST));
        }
    }

    @Override
    public String getName() throws DebugException {
        if (console == null) {
            return PydevConsole.CONSOLE_NAME;
        }
        return console.getName();
    }

    @Override
    public void initialize() {
        super.initialize();

        // We start off with a prompt active, therefore we start with the
        // virtual thread suspended.
        setSuspended(true);
    }

    @Override
    public void terminate() {
        super.terminate();
        if (console != null) {
            ScriptConsoleManager.getInstance().close(console);
        }
    }

    public void setConsole(ScriptConsole console) {
        this.console = console;
    }
}
