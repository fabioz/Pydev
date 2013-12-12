/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.debug.ui.PyConfigureExceptionDialog;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.shared_ui.EditorUtils;

public class PyConfigureExceptionAction extends PyAction implements IWorkbenchWindowActionDelegate {

    public void run(IAction action) {

        PyConfigureExceptionDialog dialog = new PyConfigureExceptionDialog(EditorUtils.getShell(), "",
                new PyExceptionListProvider(), new LabelProvider(), "");

        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        dialog.setInitialElementSelections(instance.getExceptionsList());
        dialog.setTitle("Add Python Exception Breakpoint");
        if (dialog.open() == PyConfigureExceptionDialog.OK) {

            Object[] selectedItems = dialog.getResult();
            String[] exceptionArray;
            if (selectedItems != null) {
                exceptionArray = new String[selectedItems.length];
                System.arraycopy(selectedItems, 0, exceptionArray, 0, selectedItems.length);
            } else {
                exceptionArray = new String[0];
            }

            //must be done before setBreakOn (where listeners will be notified).
            instance.setSkipCaughtExceptionsInSameFunction(dialog.getResultStopOnExceptionsHandledInSameContext());
            instance.setIgnoreExceptionsThrownInLinesWithIgnoreException(dialog
                    .getResultIgnoreExceptionsThrownInLinesWithIgnoreException());

            instance.setBreakOn(dialog.getResultHandleCaughtExceptions(),
                    dialog.getResultHandleUncaughtExceptions(), exceptionArray);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }
}
