/**
 * Copyright (c) 20014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.core.log.Log;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

import com.python.pydev.debug.remote.client_api.PyDevRemoteDebuggerAttachToProcess;
import com.python.pydev.debug.remote.client_api.PydevRemoteDebuggerServer;

public class AttachToProcess implements IWorkbenchWindowActionDelegate {

    public AttachToProcess() {
    }

    @Override
    public void run(IAction action) {
        try {
            doIt();
        } catch (Exception e) {
            Log.log(e);
            PyDialogHelpers.openCritical("Error attaching to process", e.getMessage());
        }
    }

    protected void doIt() throws Exception {
        int pid = PyDevRemoteDebuggerAttachToProcess.selectProcess("*python*");
        if (pid != -1) {
            if (!PydevRemoteDebuggerServer.isRunning()) {
                // I.e.: the remote debugger server must be on so that we can attach to it.
                PydevRemoteDebuggerServer.startServer();
            }
            PyDevRemoteDebuggerAttachToProcess.attachProcess(pid, false);

        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
    }
}