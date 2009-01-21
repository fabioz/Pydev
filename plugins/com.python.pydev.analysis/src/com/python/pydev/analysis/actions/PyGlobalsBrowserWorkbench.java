/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package com.python.pydev.analysis.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class PyGlobalsBrowserWorkbench implements IWorkbenchWindowActionDelegate {

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }

    public void run(IAction action) {
        PyGlobalsBrowser.getFromWorkspace(null);
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
