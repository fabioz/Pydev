package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class RelaunchLastAction implements IEditorActionDelegate {

    public void run(IAction action) {
        RestartLaunchAction.relaunchLast();
    }


    public void selectionChanged(IAction action, ISelection selection) {

    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {

    }

}
