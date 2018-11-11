/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterPreferencesPage;

/**
 * @author Fabio Zadrozny
 */
public class PySelectInterpreter extends PyAction {

    @Override
    public void run(IAction action) {
        try {
            PyEdit editor = getPyEdit();
            IPythonNature nature = editor.getPythonNature();
            if (nature != null) {
                IInterpreterManager interpreterManager = nature.getRelatedInterpreterManager();

                final IInterpreterInfo[] interpreterInfos = interpreterManager.getInterpreterInfos();
                if (interpreterInfos == null || interpreterInfos.length == 0) {
                    PyDialogHelpers.openWarning("No interpreters available",
                            "Unable to change default interpreter because no interpreters are available (add more interpreters in the related preferences page).");
                    return;
                }
                if (interpreterInfos.length == 1) {
                    PyDialogHelpers.openWarning("Only 1 interpreters available",
                            "Unable to change default interpreter because only 1 interpreter is configured (add more interpreters in the related preferences page).");
                    return;
                }
                // Ok, more than 1 found.
                IWorkbenchWindow workbenchWindow = EditorUtils.getActiveWorkbenchWindow();
                Assert.isNotNull(workbenchWindow);
                SelectionDialog listDialog = AbstractInterpreterPreferencesPage.createChooseIntepreterInfoDialog(
                        workbenchWindow, interpreterInfos,
                        "Select interpreter to be made the default.", false);

                int open = listDialog.open();
                if (open != ListDialog.OK || listDialog.getResult().length != 1) {
                    return;
                }
                Object[] result = listDialog.getResult();
                if (result == null || result.length == 0) {
                    return;

                }
                final IInterpreterInfo selectedInterpreter = ((IInterpreterInfo) result[0]);
                if (selectedInterpreter != interpreterInfos[0]) {
                    // Ok, some interpreter (which wasn't already the default) was selected.
                    Arrays.sort(interpreterInfos, (a, b) -> {
                        if (a == selectedInterpreter) {
                            return -1;
                        }
                        if (b == selectedInterpreter) {
                            return 1;
                        }
                        return 0; // Don't change order for the others.
                    });

                    Shell shell = EditorUtils.getShell();

                    setInterpreterInfosWithProgressDialog(interpreterManager, interpreterInfos, shell);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void setInterpreterInfosWithProgressDialog(IInterpreterManager interpreterManager,
            final IInterpreterInfo[] interpreterInfos, Shell shell) {
        //this is the default interpreter
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(shell);
        monitorDialog.setBlockOnOpen(false);

        try {
            IRunnableWithProgress operation = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
                    try {
                        Set<String> interpreterNamesToRestore = new HashSet<>(); // i.e.: don't restore the PYTHONPATH (only order was changed).
                        interpreterManager.setInfos(interpreterInfos, interpreterNamesToRestore, monitor);
                    } finally {
                        monitor.done();
                    }
                }
            };

            monitorDialog.run(true, true, operation);

        } catch (Exception e) {
            Log.log(e);
        }
    }
}
