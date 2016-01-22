/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTargetConsole;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.ListenConnector;
import org.python.pydev.debug.model.remote.RemoteDebuggerConsole;
import org.python.pydev.debug.newconsole.env.JythonEclipseProcess;
import org.python.pydev.debug.newconsole.env.PydevIProcessFactory;
import org.python.pydev.debug.newconsole.env.PydevIProcessFactory.PydevConsoleLaunchInfo;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleManager;

/**
 * Could ask to configure the interpreter in the preferences
 *
 * PreferencesUtil.createPreferenceDialogOn(null, preferencePageId, null, null)
 *
 * This is the class responsible for creating the console (and setting up the communication
 * between the console server and the client).
 *
 * @author Fabio
 */
public class PydevConsoleFactory implements IConsoleFactory {

    /**
     * @see IConsoleFactory#openConsole()
     */
    public void openConsole() {
        createConsole(null);
    }

    /**
     * Create a new PyDev console
     */
    public void createConsole(String additionalInitialComands) {
        try {
            PydevConsoleInterpreter interpreter = createDefaultPydevInterpreter();
            if (interpreter == null) {
                return;
            }
            if (interpreter.getFrame() == null) {
                createConsole(interpreter, additionalInitialComands);
            } else {
                createDebugConsole(interpreter.getFrame(), additionalInitialComands, true, true,
                        new AnyPyStackFrameSelected());
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void createConsole(final PydevConsoleInterpreter interpreter, final String additionalInitialComands) {

        Job job = new Job("Create Interactive Console") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Create Interactive Console", 4);
                try {
                    sayHello(interpreter, new SubProgressMonitor(monitor, 1));
                    connectDebugger(interpreter, additionalInitialComands, new SubProgressMonitor(monitor, 2));
                    enableGuiEvents(interpreter, new SubProgressMonitor(monitor, 1));
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    try {
                        interpreter.close();
                    } catch (Exception e_inner) {
                        // ignore, but log nested exception
                        Log.log(e_inner);
                    }
                    if (e instanceof UserCanceledException) {
                        return Status.CANCEL_STATUS;
                    } else {
                        Log.log(e);
                        return PydevDebugPlugin.makeStatus(IStatus.ERROR, "Error initializing console.", e);
                    }

                } finally {
                    monitor.done();
                }

            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void enableGuiEvents(PydevConsoleInterpreter interpreter, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Enabling GUI Event Loop", 1);
        try {

            PydevConsoleCommunication consoleCommunication = (PydevConsoleCommunication) interpreter
                    .getConsoleCommunication();
            String enableGuiOnStartup = InteractiveConsolePrefs.getEnableGuiOnStartup();
            consoleCommunication.enableGui(enableGuiOnStartup);
        } catch (Exception ex) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                    "Failed to set GUI event loop integration", ex));
        } finally {
            monitor.done();
        }
    }

    private void sayHello(PydevConsoleInterpreter interpreter, IProgressMonitor monitor)
            throws UserCanceledException, CoreException {
        monitor.beginTask("Connect To PyDev Console Process", 1);
        try {

            PydevConsoleCommunication consoleCommunication = (PydevConsoleCommunication) interpreter
                    .getConsoleCommunication();

            try {
                consoleCommunication.hello(new SubProgressMonitor(monitor, 1));
            } catch (UserCanceledException uce) {
                throw uce;
            } catch (Exception ex) {
                final String message;
                if (ex instanceof SocketTimeoutException) {
                    message = "Timed out after " + InteractiveConsolePrefs.getMaximumAttempts()
                            + " attempts to connect to the console.";
                } else {
                    message = "Unexpected error connecting to console.";
                }
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, message, ex));
            }
        } finally {
            monitor.done();
        }
    }

    private void connectDebugger(final PydevConsoleInterpreter interpreter, final String additionalInitialComands,
            IProgressMonitor monitor) throws IOException, CoreException, DebugException, UserCanceledException {
        monitor.beginTask("Connect Debugger", 10);
        try {
            if (interpreter.getFrame() == null) {
                monitor.worked(1);
                PydevConsole console = new PydevConsole(interpreter, additionalInitialComands);
                monitor.worked(1);
                createDebugTarget(interpreter, console, new SubProgressMonitor(monitor, 8));
                ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
                manager.add(console, true);
            }
        } finally {
            monitor.done();
        }
    }

    private void createDebugTarget(PydevConsoleInterpreter interpreter, PydevConsole console, IProgressMonitor monitor)
            throws IOException, CoreException, DebugException, UserCanceledException {
        monitor.beginTask("Connect Debug Target", 2);
        try {
            // Jython within Eclipse does not yet support debugging
            // NOTE: Jython within Eclipse currently works "once", i.e. it sets up properly and you can debug your
            // scripts you run within Eclipse, but the termination does not work properly and it seems that
            // we don't clean-up properly. There is a small additional problem, pysrc is not on the PYTHONPATH
            // so it fails to run properly, a simple hack to the pydevconsole to add its dirname to the sys.path
            // resolves that issue though.
            Process process = interpreter.getProcess();
            if (InteractiveConsolePrefs.getConsoleConnectDebugSession() && !(process instanceof JythonEclipseProcess)) {
                PydevConsoleCommunication consoleCommunication = (PydevConsoleCommunication) interpreter
                        .getConsoleCommunication();

                int acceptTimeout = PydevPrefs.getPreferences().getInt(PydevEditorPrefs.CONNECT_TIMEOUT);
                PyDebugTargetConsole pyDebugTargetConsole = null;
                ILaunch launch = interpreter.getLaunch();
                IProcess eclipseProcess = launch.getProcesses()[0];
                RemoteDebuggerConsole debugger = new RemoteDebuggerConsole();
                ListenConnector connector = new ListenConnector(acceptTimeout);
                debugger.startConnect(connector);
                pyDebugTargetConsole = new PyDebugTargetConsole(consoleCommunication, launch,
                        eclipseProcess, debugger);

                Socket socket = null;
                try {
                    consoleCommunication.connectToDebugger(connector.getLocalPort());
                    socket = debugger.waitForConnect(monitor, process, eclipseProcess);
                    if (socket == null) {
                        throw new UserCanceledException("Cancelled");
                    }
                } catch (Exception ex) {
                    try {
                        if (ex instanceof UserCanceledException) {
                            //Only close the console communication if the user actually cancelled (otherwise the user will expect it to still be working).
                            consoleCommunication.close();
                            debugger.dispose(); //Can't terminate the process either!
                        } else {
                            //But we still want to dispose of the connector.
                            debugger.disposeConnector();
                        }
                    } catch (Exception e) {
                        // Don't hide important information from user
                        Log.log(e);
                    }
                    if (ex instanceof UserCanceledException) {
                        UserCanceledException userCancelled = (UserCanceledException) ex;
                        throw userCancelled;
                    }
                    String message = "Unexpected error setting up the debugger";
                    if (ex instanceof SocketTimeoutException) {
                        message = "Timed out after " + Float.toString(acceptTimeout / 1000)
                                + " seconds while waiting for python script to connect.";
                    }
                    throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, message, ex));
                }

                pyDebugTargetConsole.startTransmission(socket); // this starts reading/writing from sockets
                pyDebugTargetConsole.initialize();

                consoleCommunication.setDebugTarget(pyDebugTargetConsole);
                launch.addDebugTarget(pyDebugTargetConsole);
                launch.setSourceLocator(new PySourceLocator());
                ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
                launchManager.addLaunch(launch);

                pyDebugTargetConsole.setConsole(console);
                console.setProcess(pyDebugTargetConsole.getProcess());
                pyDebugTargetConsole.finishedInit = true;
            }
        } finally {
            monitor.done();
        }
    }

    public PydevDebugConsole createDebugConsole(ILaunch launch, String additionalInitialComands, boolean addToManager,
            boolean bufferedOutput, IPyStackFrameProvider consoleFrameProvider) throws Exception {
        return createDebugConsole(launch, null, additionalInitialComands, addToManager, bufferedOutput,
                consoleFrameProvider);
    }

    public PydevDebugConsole createDebugConsole(PyStackFrame frame, String additionalInitialComands,
            boolean addToManager, boolean bufferedOutput, IPyStackFrameProvider consoleFrameProvider) throws Exception {
        return createDebugConsole(null, frame, additionalInitialComands, addToManager, bufferedOutput,
                consoleFrameProvider);
    }

    /**
     * Create a new Debug Console
     *
     * @param interpreter
     * @param additionalInitialComands
     * @return
     */
    private PydevDebugConsole createDebugConsole(ILaunch launch, PyStackFrame frame, String additionalInitialComands,
            boolean addToManager, boolean bufferedOutput, IPyStackFrameProvider consoleFrameProvider) throws Exception {
        PydevConsoleLaunchInfo launchAndProcess = new PydevConsoleLaunchInfo(null, null, 0, null, frame, null, null,
                launch != null ? PydevIProcessFactory.getEncodingFromLaunch(launch)
                        : PydevIProcessFactory.getEncodingFromFrame(frame));

        PydevConsoleInterpreter interpreter = createPydevDebugInterpreter(launchAndProcess, bufferedOutput,
                consoleFrameProvider);
        PydevDebugConsole console = new PydevDebugConsole(interpreter, additionalInitialComands);

        if (addToManager) {
            ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
            manager.add(console, true);
        }
        return console;
    }

    /**
     * @return A PydevConsoleInterpreter with its communication configured.
     *
     * @throws CoreException
     * @throws IOException
     * @throws UserCanceledException
     */
    public static PydevConsoleInterpreter createDefaultPydevInterpreter()
            throws Exception, UserCanceledException {

        //            import sys; sys.ps1=''; sys.ps2=''
        //            import sys;print >> sys.stderr, ' '.join([sys.executable, sys.platform, sys.version])
        //            print >> sys.stderr, 'PYTHONPATH:'
        //            for p in sys.path:
        //                print >> sys.stderr,  p
        //
        //            print >> sys.stderr, 'Ok, all set up... Enjoy'

        PydevIProcessFactory iprocessFactory = new PydevIProcessFactory();

        PydevConsoleLaunchInfo launchAndProcess = iprocessFactory.createInteractiveLaunch();
        if (launchAndProcess == null) {
            return null;
        }
        if (launchAndProcess.interpreter != null) {
            return createPydevInterpreter(launchAndProcess, iprocessFactory.getNaturesUsed(), launchAndProcess.encoding);
        } else {
            return createPydevDebugInterpreter(launchAndProcess, true, new AnyPyStackFrameSelected());
        }

    }

    // Use IProcessFactory to get the required tuple
    public static PydevConsoleInterpreter createPydevInterpreter(PydevConsoleLaunchInfo info,
            List<IPythonNature> natures, String encoding) throws Exception {
        final ILaunch launch = info.launch;
        Process process = info.process;
        Integer clientPort = info.clientPort;
        IInterpreterInfo interpreterInfo = info.interpreter;
        if (launch == null) {
            return null;
        }

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(PydevIProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(new PydevConsoleCommunication(port, process, clientPort,
                info.cmdLine, info.env, encoding));
        consoleInterpreter.setNaturesUsed(natures);
        consoleInterpreter.setInterpreterInfo(interpreterInfo);
        consoleInterpreter.setLaunch(launch);
        consoleInterpreter.setProcess(process);

        InteractiveConsolePlugin.getDefault().addConsoleLaunch(launch);

        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                InteractiveConsolePlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    }

    /**
     * Initialize Console Interpreter and Console Communication for the Debug Console
     */
    public static PydevConsoleInterpreter createPydevDebugInterpreter(PydevConsoleLaunchInfo info,
            boolean bufferedOutput, IPyStackFrameProvider consoleFrameProvider) throws Exception {

        PyStackFrame frame = info.frame;

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        consoleInterpreter.setFrame(frame);
        consoleInterpreter.setLaunchAndRelatedInfo(info.launch);
        consoleInterpreter.setProcess(info.process);

        // pydev console uses running debugger as a backend
        consoleInterpreter.setConsoleCommunication(new PydevDebugConsoleCommunication(bufferedOutput,
                consoleFrameProvider));
        return consoleInterpreter;
    }

}
