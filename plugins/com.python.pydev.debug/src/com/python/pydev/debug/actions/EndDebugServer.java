package com.python.pydev.debug.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.python.pydev.debug.remote.client_api.PydevRemoteDebuggerServer;


public class EndDebugServer implements IWorkbenchWindowActionDelegate {
    
    public EndDebugServer() {
    }

    public void run(IAction action) {
        PydevRemoteDebuggerServer.stopServer();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }
}