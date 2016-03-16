/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;


/**
 * Helper class for executing an action and showing its progress.
 * 
 * @author Fabio Zadrozny
 */
public class ProgressOperation extends WorkspaceModifyOperation {
    private final ProgressAction action;

    public IProgressMonitor monitor;
    public int estimatedTaskUnits = 10000;

    public ProgressOperation(ProgressAction action) {
        super();
        this.action = action;
    }

    @Override
    protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
            InterruptedException {

        try {
            this.monitor = monitor;
            action.monitor = monitor;
            monitor.beginTask("Action being executed...", estimatedTaskUnits);
            action.run();
            monitor.done();
        } catch (Exception e) {
            Log.log(e);
        }

    }

    /**
     * @param shell
     * 
     */
    public static void startAction(Shell shell, ProgressAction action, boolean cancelable) {
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(shell);
        monitorDialog.setCancelable(cancelable);
        monitorDialog.setBlockOnOpen(false);
        try {
            IRunnableWithProgress operation = new ProgressOperation(action);
            monitorDialog.run(false, false, operation);
            // Perform the action
        } catch (InvocationTargetException e) {
            Log.log(e);
        } catch (InterruptedException e) {
            Log.log(e);
        }

    }
}
