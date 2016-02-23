/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;
import com.python.pydev.ui.hierarchy.PyHierarchyView;

/**
 * 
 * Based on 
 * org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction
 * org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil
 * @author fabioz
 *
 */
public class PyShowHierarchy extends PyRefactorAction {

    @Override
    protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
        try {
            final PyHierarchyView view;
            IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            IWorkbenchPage page = workbenchWindow.getActivePage();
            view = (PyHierarchyView) page.showView("com.python.pydev.ui.hierarchy.PyHierarchyView", null,
                    IWorkbenchPage.VIEW_VISIBLE);

            ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(EditorUtils.getShell());
            try {
                IRunnableWithProgress operation = new IRunnableWithProgress() {

                    @Override
                    public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                            InterruptedException {
                        try {
                            final HierarchyNodeModel model;

                            //set whatever is needed for the hierarchy
                            IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                            if (pyRefactoring instanceof IPyRefactoring2) {
                                RefactoringRequest refactoringRequest = getRefactoringRequest(monitor);
                                IPyRefactoring2 r2 = (IPyRefactoring2) pyRefactoring;
                                model = r2.findClassHierarchy(refactoringRequest, false);

                                if (monitor.isCanceled()) {
                                    return;
                                }
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!monitor.isCanceled()) {
                                            view.setHierarchy(model);
                                        }
                                    }
                                };
                                Display.getDefault().asyncExec(r);
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }

                    }
                };

                boolean fork = true;
                monitorDialog.run(fork, true, operation);
            } catch (Throwable e) {
                Log.log(e);
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return "";
    }

}
