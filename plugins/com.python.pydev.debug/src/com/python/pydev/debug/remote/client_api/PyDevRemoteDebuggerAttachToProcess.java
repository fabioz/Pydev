package com.python.pydev.debug.remote.client_api;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.dialogs.Select1Dialog;
import org.python.pydev.ui.dialogs.TreeNodeLabelProvider;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterPreferencesPage;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.actions.ShowProcessOutputDialog;

public class PyDevRemoteDebuggerAttachToProcess {

    /**
     * Show an dialog to let user select an process to attach
     * 
     * @param filter It's a string like "*python*"
     * @return The selected process id, -1 means no valid process
     */
    public static int selectProcess(String filter) {
        IProcessList processList = PlatformUtils.getProcessList();
        IProcessInfo[] processList2 = processList.getProcessList();
        TreeNode<Object> root = new TreeNode<Object>(null, null);
        for (IProcessInfo iProcessInfo : processList2) {
            new TreeNode<>(root, iProcessInfo);
        }
        TreeNode<Object> element = new Select1Dialog() {
            @Override
            protected String getInitialFilter() {
                return filter;
            };

            @Override
            protected ILabelProvider getLabelProvider() {
                return new TreeNodeLabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return ImageCache.asImage(SharedUiPlugin.getImageCache().get(UIConstants.PUBLIC_ATTR_ICON));
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

        int pid = -1;
        if (element != null) {
            IProcessInfo p = (IProcessInfo) element.data;
            pid = p.getPid();

        }
        return pid;

    }

    /**
     * Attach to the process
     * 
     * @param pid The process to be attached
     * @param skipDialogIfPossible If true, don't show selection dialog if possible 
     * @throws Exception
     */
    public static void attachProcess(int pid, boolean skipDialogIfPossible) throws Exception {

        // select interpreter
        IInterpreterManager interpreterManager = InterpreterManagersAPI.getPythonInterpreterManager();
        IInterpreterInfo interpreter = selectInterpreter(interpreterManager, skipDialogIfPossible);

        SimplePythonRunner runner = new SimplePythonRunner();
        IPath relative = new Path("pysrc").append("pydevd_attach_to_process").append("attach_pydevd.py");
        String script = CorePlugin.getBundleInfo().getRelativePath(relative).getAbsolutePath();
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

    /**
     * Select the Python interpreter
     * 
     * @param interpreterManager
     * @param skipDialogIfPossible true, if there is only one interpreter, select it by default, no dialog will show 
     * @return The selected Python interpreter
     */
    private static IInterpreterInfo selectInterpreter(IInterpreterManager interpreterManager,
            boolean skipDialogIfPossible) {

        //Select interpreter
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (interpreterManager == null) {
            MessageDialog.openError(workbenchWindow.getShell(), "No interpreter manager.",
                    "No interpreter manager was available for attaching to a process.");
        }
        IInterpreterInfo[] interpreters = interpreterManager.getInterpreterInfos();
        if (interpreters == null || interpreters.length == 0) {
            MessageDialog
                    .openError(workbenchWindow.getShell(), "No interpreters for creating console",
                            "An interpreter that matches the architecture of the target process must be configured in the interpreter preferences.");
            return null;
        }

        IInterpreterInfo interpreter = null;

        if (interpreters.length == 1 && skipDialogIfPossible) {
            interpreter = interpreters[0];

        } else {

            SelectionDialog listDialog = AbstractInterpreterPreferencesPage.createChooseIntepreterInfoDialog(
                    workbenchWindow, interpreters,
                    "Select interpreter which matches the architecture of the target process (i.e.: 32/64 bits).",
                    false);

            int open = listDialog.open();
            if (open != ListDialog.OK || listDialog.getResult().length != 1) {
                return null;
            }
            Object[] result = listDialog.getResult();

            if (result == null || result.length == 0) {
                interpreter = interpreters[0];

            } else {
                interpreter = ((IInterpreterInfo) result[0]);
            }
        }

        return interpreter;

    }

}
