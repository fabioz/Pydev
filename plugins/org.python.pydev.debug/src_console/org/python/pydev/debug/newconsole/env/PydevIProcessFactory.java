/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 20, 2006
 */
package org.python.pydev.debug.newconsole.env;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsoleUMDPrefs;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleIronpythonRunner;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.net.SocketUtil;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterPreferencesPage;

/**
 * This class is used to create the given IProcess and get the console that is attached to that process.
 */
public class PydevIProcessFactory {

    public static final class PydevConsoleLaunchInfo {
        public final Launch launch;
        public final Process process;
        public final int clientPort;
        public final IInterpreterInfo interpreter;
        public final PyStackFrame frame;
        public final String[] cmdLine;
        public final String[] env;
        public final String encoding;

        /**
         * @param launch
         * @param process
         * @param clientPort
         * @param interpreter
         * @param frame
         * @param env
         * @param cmdLine
         */
        public PydevConsoleLaunchInfo(Launch launch, Process process, int clientPort, IInterpreterInfo interpreter,
                PyStackFrame frame, String[] cmdLine, String[] env, String encoding) {
            this.launch = launch;
            this.process = process;
            this.clientPort = clientPort;
            this.interpreter = interpreter;
            this.frame = frame;
            this.cmdLine = cmdLine;
            this.env = env;
            this.encoding = encoding;
        }
    }

    private List<IPythonNature> naturesUsed;

    public List<IPythonNature> getNaturesUsed() {
        return naturesUsed;
    }

    /**
     * @return a shell that we can use.
     */
    public Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static final String INTERACTIVE_LAUNCH_PORT = "INTERACTIVE_LAUNCH_PORT";

    /**
     * Creates a launch (and its associated IProcess) for the xml-rpc server to be used in the interactive console.
     *
     * It'll ask the user how to create it:
     * - editor
     * - python interpreter
     * - jython interpreter
     *
     * @return the Launch, the Process created and the port that'll be used for the server to call back into
     * this client for requesting input.
     *
     * @throws UserCanceledException
     * @throws Exception
     */
    public PydevConsoleLaunchInfo createInteractiveLaunch() throws UserCanceledException, Exception {

        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        IEditorPart activeEditor = activePage.getActiveEditor();
        PyEdit edit = null;

        if (activeEditor instanceof PyEdit) {
            edit = (PyEdit) activeEditor;
        }

        ChooseProcessTypeDialog dialog = new ChooseProcessTypeDialog(getShell(), edit);
        if (dialog.open() == ChooseProcessTypeDialog.OK) {

            PyStackFrame selectedFrame = dialog.getSelectedFrame();
            if (selectedFrame != null) {
                // Interpreter not required for Debug Console
                String encoding = getEncodingFromFrame(selectedFrame);

                return new PydevConsoleLaunchInfo(null, null, 0, null, selectedFrame,
                        new String[] { "Debug connection (no command line)" }, null, encoding);
            }

            IInterpreterManager interpreterManager = dialog.getInterpreterManager();
            if (interpreterManager == null) {
                MessageDialog.openError(workbenchWindow.getShell(), "No interpreter manager for creating console",
                        "No interpreter manager was available for creating a console.");
            }
            IInterpreterInfo[] interpreters = interpreterManager.getInterpreterInfos();
            if (interpreters == null || interpreters.length == 0) {
                MessageDialog.openError(workbenchWindow.getShell(), "No interpreters for creating console",
                        "No interpreter available for creating a console.");
                return null;
            }
            IInterpreterInfo interpreter = null;
            if (interpreters.length == 1) {
                //We just have one, so, no point in asking about which one should be there.
                interpreter = interpreters[0];
            }

            if (interpreter == null) {
                SelectionDialog listDialog = AbstractInterpreterPreferencesPage.createChooseIntepreterInfoDialog(
                        workbenchWindow, interpreters, "Select interpreter to be used.", false);

                int open = listDialog.open();
                if (open != ListDialog.OK || listDialog.getResult().length > 1) {
                    return null;
                }
                Object[] result = listDialog.getResult();
                if (result == null || result.length == 0) {
                    interpreter = interpreters[0];

                } else {
                    interpreter = ((IInterpreterInfo) result[0]);
                }
            }

            if (interpreter == null) {
                return null;
            }

            Tuple<Collection<String>, IPythonNature> pythonpathAndNature = dialog.getPythonpathAndNature(interpreter);
            if (pythonpathAndNature == null) {
                return null;
            }

            return createLaunch(interpreterManager, interpreter, pythonpathAndNature.o1, pythonpathAndNature.o2,
                    dialog.getNatures());
        }
        return null;
    }

    public static String getEncodingFromFrame(PyStackFrame selectedFrame) {
        try {
            IDebugTarget adapter = (IDebugTarget) selectedFrame.getAdapter(IDebugTarget.class);
            if (adapter == null) {
                return "UTF-8";
            }
            IProcess process = adapter.getProcess();
            if (process == null) {
                return "UTF-8";
            }
            ILaunch launch = process.getLaunch();
            if (launch == null) {
                Log.log("Unable to get launch for: " + process);
                return "UTF-8";
            }
            return getEncodingFromLaunch(launch);
        } catch (Exception e) {
            Log.log(e);
            return "UTF-8";
        }
    }

    public static String getEncodingFromLaunch(ILaunch launch) {
        try {
            String encoding = launch.getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING);
            if (encoding == null) {
                Log.log("Unable to get: " + DebugPlugin.ATTR_CONSOLE_ENCODING + " from launch.");
                return "UTF-8";
            }
            return encoding;
        } catch (Exception e) {
            Log.log(e);
            return "UTF-8";
        }
    }

    private static ILaunchConfiguration createLaunchConfig() {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = manager
                .getLaunchConfigurationType("org.python.pydev.debug.interactiveConsoleConfigurationType");
        ILaunchConfigurationWorkingCopy newInstance;
        try {
            newInstance = launchConfigurationType.newInstance(null,
                    manager.generateLaunchConfigurationName("PyDev Interactive Launch"));
        } catch (CoreException e) {
            return null;
        }
        newInstance.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
        return newInstance;
    }

    public PydevConsoleLaunchInfo createLaunch(IInterpreterManager interpreterManager, IInterpreterInfo interpreter,
            Collection<String> pythonpath, IPythonNature nature, List<IPythonNature> naturesUsed) throws Exception {
        Process process = null;
        this.naturesUsed = naturesUsed;
        Integer[] ports = SocketUtil.findUnusedLocalPorts(2);
        int port = ports[0];
        int clientPort = ports[1];

        final Launch launch = new Launch(createLaunchConfig(), "interactive", null);
        launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false");
        launch.setAttribute(INTERACTIVE_LAUNCH_PORT, "" + port);

        File scriptWithinPySrc = PydevPlugin.getScriptWithinPySrc("pydevconsole.py");
        String pythonpathEnv = SimpleRunner.makePythonPathEnvFromPaths(pythonpath);
        String[] commandLine;
        switch (interpreterManager.getInterpreterType()) {

            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                commandLine = SimplePythonRunner.makeExecutableCommandStr(interpreter.getExecutableOrJar(),
                        scriptWithinPySrc.getAbsolutePath(),
                        new String[] { String.valueOf(port), String.valueOf(clientPort) });
                break;

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                commandLine = SimpleIronpythonRunner.makeExecutableCommandStr(interpreter.getExecutableOrJar(),
                        scriptWithinPySrc.getAbsolutePath(),
                        new String[] { String.valueOf(port), String.valueOf(clientPort) });
                break;

            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                String vmArgs = PydevDebugPlugin.getDefault().getPreferenceStore()
                        .getString(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS);

                commandLine = SimpleJythonRunner.makeExecutableCommandStrWithVMArgs(interpreter.getExecutableOrJar(),
                        scriptWithinPySrc.getAbsolutePath(), pythonpathEnv, vmArgs, new String[] {
                                String.valueOf(port), String.valueOf(clientPort) });
                break;

            case IInterpreterManager.INTERPRETER_TYPE_JYTHON_ECLIPSE:
                commandLine = null;
                break;

            default:
                throw new RuntimeException(
                        "Expected interpreter manager to be Python or Jython or IronPython related.");
        }
        String[] cmdLine;
        String[] env;

        String encoding = PydevDebugPlugin.getDefault().getPreferenceStore()
                .getString(PydevConsoleConstants.INTERACTIVE_CONSOLE_ENCODING);
        if (encoding.trim().length() == 0) {
            encoding = "UTF-8"; //Default is utf-8
        }

        if (interpreterManager.getInterpreterType() == IInterpreterManager.INTERPRETER_TYPE_JYTHON_ECLIPSE) {
            process = new JythonEclipseProcess(scriptWithinPySrc.getAbsolutePath(), port, clientPort);
            cmdLine = new String[] { "Internal Jython process (no command line)" };
            env = null;

        } else {
            env = SimpleRunner.createEnvWithPythonpath(pythonpathEnv, interpreter.getExecutableOrJar(),
                    interpreterManager, nature);
            // Add in UMD settings
            String[] s = new String[env.length + 4];
            System.arraycopy(env, 0, s, 0, env.length);

            s[s.length - 4] = "PYTHONIOENCODING=" + encoding;
            s[s.length - 3] = "PYDEV_UMD_ENABLED="
                    + Boolean.toString(InteractiveConsoleUMDPrefs.isUMDEnabled());
            s[s.length - 2] = "PYDEV_UMD_NAMELIST="
                    + InteractiveConsoleUMDPrefs.getUMDExcludeModules();
            s[s.length - 1] = "PYDEV_UMD_VERBOSE="
                    + Boolean.toString(InteractiveConsoleUMDPrefs.isUMDVerbose());
            env = s;
            cmdLine = commandLine;

            process = SimpleRunner.createProcess(commandLine, env, null);
        }

        IProcess newProcess = new PydevSpawnedInterpreterProcess(launch, process, interpreter.getNameForUI(), encoding);

        launch.addProcess(newProcess);

        return new PydevConsoleLaunchInfo(launch, process, clientPort, interpreter, null, cmdLine, env, encoding);
    }

}
