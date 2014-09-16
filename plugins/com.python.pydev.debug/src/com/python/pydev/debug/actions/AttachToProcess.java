/**
 * Copyright (c) 20014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.actions;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.dialogs.Select1Dialog;
import org.python.pydev.ui.dialogs.TreeNodeLabelProvider;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterPreferencesPage;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.remote.client_api.PydevRemoteDebuggerServer;

public class AttachToProcess implements IWorkbenchWindowActionDelegate {

    public AttachToProcess() {
    }

    public void run(IAction action) {
        try {
            doIt();
        } catch (Exception e) {
            Log.log(e);
            PyDialogHelpers.openCritical("Error attaching to process", e.getMessage());
        }
    }

    protected void doIt() throws Exception {
        IProcessList processList = PlatformUtils.getProcessList();
        IProcessInfo[] processList2 = processList.getProcessList();
        TreeNode<Object> root = new TreeNode<Object>(null, null);
        for (IProcessInfo iProcessInfo : processList2) {
            new TreeNode<>(root, iProcessInfo);
        }
        TreeNode<Object> element = new Select1Dialog() {
            @Override
            protected String getInitialFilter() {
                return "*python*";
            };

            @Override
            protected ILabelProvider getLabelProvider() {
                return new TreeNodeLabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return SharedUiPlugin.getImageCache().get(UIConstants.PUBLIC_ATTR_ICON);
                    };

                    @SuppressWarnings("unchecked")
                    @Override
                    public String getText(Object element) {
                        if (element == null) {
                            return "null";
                        }
                        TreeNode<Object> node = (TreeNode<Object>) element;
                        Object data = node.data;
                        if (data instanceof IProcessInfo) {
                            IProcessInfo iProcessInfo = (IProcessInfo) data;
                            return iProcessInfo.getPid() + " - " + iProcessInfo.getName();
                        }
                        return "Unexpected: " + data;
                    };
                };
            };
        }.selectElement(root);
        if (element != null) {
            IProcessInfo p = (IProcessInfo) element.data;
            int pid = p.getPid();
            if (!PydevRemoteDebuggerServer.isRunning()) {
                // I.e.: the remote debugger server must be on so that we can attach to it.
                PydevRemoteDebuggerServer.startServer();
            }

            //Select interpreter
            IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            IInterpreterManager interpreterManager = PydevPlugin.getPythonInterpreterManager();
            if (interpreterManager == null) {
                MessageDialog.openError(workbenchWindow.getShell(), "No interpreter manager.",
                        "No interpreter manager was available for attaching to a process.");
            }
            IInterpreterInfo[] interpreters = interpreterManager.getInterpreterInfos();
            if (interpreters == null || interpreters.length == 0) {
                MessageDialog
                        .openError(workbenchWindow.getShell(), "No interpreters for creating console",
                                "An interpreter that matches the architecture of the target process must be configured in the interpreter preferences.");
                return;
            }
            SelectionDialog listDialog = AbstractInterpreterPreferencesPage.createChooseIntepreterInfoDialog(
                    workbenchWindow, interpreters,
                    "Select interpreter which matches the architecture of the target process (i.e.: 32/64 bits).",
                    false);

            int open = listDialog.open();
            if (open != ListDialog.OK || listDialog.getResult().length != 1) {
                return;
            }
            Object[] result = listDialog.getResult();
            IInterpreterInfo interpreter;
            if (result == null || result.length == 0) {
                interpreter = interpreters[0];

            } else {
                interpreter = ((IInterpreterInfo) result[0]);
            }
            SimplePythonRunner runner = new SimplePythonRunner();
            IPath relative = new Path("pysrc").append("pydevd_attach_to_process").append("attach_pydevd.py");
            String script = PydevPlugin.getBundleInfo().getRelativePath(relative).getAbsolutePath();
            String[] args = new String[] {
                    "--port",
                    "" + DebugPluginPrefsInitializer.getRemoteDebuggerPort(),
                    "--pid",
                    "" + pid
            };

            IPythonNature nature = new SystemPythonNature(interpreterManager, interpreter);
            String[] s = SimplePythonRunner.preparePythonCallParameters(interpreter.getExecutableOrJar(), script, args);
            Tuple<Process, String> run = runner.run(s, (File) null, nature, new NullProgressMonitor());
            if (run.o1 != null) {
                ShowProcessOutputDialog dialog = new ShowProcessOutputDialog(UIUtils.getActiveShell(), run.o1);
                dialog.open();
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }
}