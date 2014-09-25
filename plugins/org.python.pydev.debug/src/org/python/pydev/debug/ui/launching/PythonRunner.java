/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 18, 2004
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.ListenConnector;
import org.python.pydev.debug.model.remote.RemoteDebugger;
import org.python.pydev.debug.processfactory.PyProcessFactory;
import org.python.pydev.debug.pyunit.IPyUnitServer;
import org.python.pydev.debug.pyunit.PyUnitServer;
import org.python.pydev.debug.pyunit.PyUnitView;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_ui.debug.RelaunchConstants;

/**
 * Launches Python process, and connects it to Eclipse's debugger.
 * Waits for process to complete.
 * 
 * Modeled after org.eclipse.jdt.internal.launching.StandardVMDebugger.
 */
public class PythonRunner {

    /**
     * To listen to changes on PyUnit runs, one would do:
     
        ICallbackListener<IPyUnitServer> listener = new ICallbackListener<IPyUnitServer>() {
            
            public Object call(IPyUnitServer pyUnitServer) {
                IPyUnitServerListener pyUnitViewServerListener = new IPyUnitServerListener() {
                    
                    public void notifyTestsCollected(String totalTestsCount) {
                    }
                    
                    public void notifyTest(String status, String location, String test, String capturedOutput, String errorContents,
                            String time) {
                    }
                    
                    public void notifyStartTest(String location, String test) {
                    }
                    
                    public void notifyFinished(String totalTimeInSecs) {
                    }
                    
                    public void notifyDispose() {
                    }
                };
                pyUnitServer.registerOnNotifyTest(pyUnitViewServerListener);
                return null;
            }
        };
        PythonRunner.onPyUnitServerCreated.registerListener(listener);

     */
    public static final CallbackWithListeners<IPyUnitServer> onPyUnitServerCreated = new CallbackWithListeners<IPyUnitServer>();

    /**
     * Launches the configuration
     * 
     * The code is modeled after Ant launching example.
     */
    public static void run(final PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor)
            throws CoreException, IOException {

        try {
            PyUnitServer pyUnitServer = null;
            if (config.isUnittest()) {
                pyUnitServer = config.createPyUnitServer(config, launch);
                onPyUnitServerCreated.call(pyUnitServer);
                PyUnitView.registerPyUnitServer(pyUnitServer);
            }

            if (config.isDebug) {
                runDebug(config, launch, monitor);

            } else { //default - just configured by command line (the others need special attention)
                doIt(config, monitor, config.envp, config.getCommandLine(true), config.workingDirectory, launch);
            }

        } catch (final JDTNotAvailableException e) {
            Log.log(e);
            final Display display = Display.getDefault();
            display.syncExec(new Runnable() {

                public void run() {
                    MessageDialog.openError(display.getActiveShell(), "Unable to run the selected configuration.",
                            e.getMessage());
                }

            });
        }
    }

    /**
     * Launches the config in the debug mode.
     * 
     * Loosely modeled upon Ant launcher.
     * @throws JDTNotAvailableException 
     */
    private static void runDebug(final PythonRunnerConfig config, final ILaunch launch, IProgressMonitor monitor)
            throws CoreException, IOException, JDTNotAvailableException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
        subMonitor.beginTask("Launching python", 1);

        // Launch & connect to the debugger        
        final RemoteDebugger debugger = new RemoteDebugger();
        final ListenConnector listenConnector = debugger.startConnect(subMonitor, config);
        subMonitor.subTask("Constructing command_line...");
        String[] cmdLine = config.getCommandLine(true);

        Process p = createProcess(launch, config.envp, cmdLine, config.workingDirectory);
        HashMap<String, String> processAttributes = new HashMap<>();
        processAttributes.put(IProcess.ATTR_CMDLINE, config.getCommandLineAsString());
        processAttributes.put(Constants.PYDEV_DEBUG_IPROCESS_ATTR, Constants.PYDEV_DEBUG_IPROCESS_ATTR_TRUE);

        //Set the debug target before registering with the debug plugin (we want it before creating the console).
        PyDebugTarget t = new PyDebugTarget(launch, null, config.resource, debugger, config.project);
        IProcess process;
        try {
            process = registerWithDebugPluginForProcessType(config.getRunningName(), launch, p, processAttributes,
                    config);
            t.process = process;
        } finally {
            t.finishedInit = true;
        }

        subMonitor.subTask("Waiting for connection...");
        Socket socket = null;
        try {
            socket = debugger.waitForConnect(subMonitor, p, process);
            if (socket == null) {
                debugger.dispose();
                return;
            }
        } catch (Exception ex) {
            process.terminate();
            p.destroy();
            String message = "Unexpected error setting up the debugger";
            if (ex instanceof SocketTimeoutException) {
                message = "Timed out after " + Float.toString(config.acceptTimeout / 1000)
                        + " seconds while waiting for python script to connect.";
            }
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, message, ex));
        }
        subMonitor.subTask("Done");
        // hook up debug model, and we are off & running
        launch.setSourceLocator(new PySourceLocator());
        t.startTransmission(socket); // this starts reading/writing from sockets
        t.initialize();
        t.addConsoleInputListener();

        // Accept new connections in the same socket so that child processes can connect too.
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    listenConnector.setTimeout(0);
                    while (!launch.isTerminated() && !listenConnector.isDisposed()) {
                        Socket socket = listenConnector.waitForConnection();
                        if (socket != null && !socket.isClosed()) {
                            PyDebugTarget t = new PyDebugTarget(launch, null, config.resource, debugger, config.project);
                            try {
                                t.startTransmission(socket); // this starts reading/writing from sockets
                                t.initialize();
                            } catch (IOException e) {
                                Log.log(e);
                            }
                        }
                    }
                } catch (SocketException e) {
                    //Ok: socket closed.
                } catch (Exception e) {
                    Log.log(e);
                }

            }

        };
        thread.setDaemon(true);
        thread.start();

    }

    private static IProcess doIt(PythonRunnerConfig config, IProgressMonitor monitor, String[] envp, String[] cmdLine,
            File workingDirectory, ILaunch launch) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);

        subMonitor.beginTask("Launching python", 1);

        // Launch & connect to the debugger        
        subMonitor.subTask("Constructing command_line...");
        String commandLineAsString = SimpleRunner.getArgumentsAsStr(cmdLine);
        //System.out.println("running command line: "+commandLineAsString);
        Map<String, String> processAttributes = new HashMap<>();

        processAttributes.put(IProcess.ATTR_CMDLINE, commandLineAsString);

        subMonitor.subTask("Exec...");

        //it was dying before register, so, I made this faster to see if this fixes it
        Process p = createProcess(launch, envp, cmdLine, workingDirectory);

        IProcess process;
        String label = cmdLine[cmdLine.length - 1];

        //in the interactive session, we'll just create the process, it won't actually be registered
        //in the debug plugin (the communication is all done through xml-rpc).
        if (config.isInteractive) {
            throw new RuntimeException("Interactive not supported here!");
        }
        process = registerWithDebugPluginForProcessType(label, launch, p, processAttributes, config);

        // Registered the process with the debug plugin
        subMonitor.subTask("Done");
        return process;
    }

    /**
     * Actually creates the process (and create the encoding config file)
     */
    @SuppressWarnings("deprecation")
    private static Process createProcess(ILaunch launch, String[] envp, String[] cmdLine, File workingDirectory)
            throws CoreException {
        Map<String, String> arrayAsMapEnv = ProcessUtils.getArrayAsMapEnv(envp);
        arrayAsMapEnv.put("PYTHONUNBUFFERED", "1");
        arrayAsMapEnv.put("PYDEV_COMPLETER_PYTHONPATH", PydevPlugin.getBundleInfo().getRelativePath(new Path("pysrc"))
                .toString());

        //Not using DebugPlugin.ATTR_CONSOLE_ENCODING to provide backward compatibility for eclipse 3.2
        String encoding = launch.getAttribute(IDebugUIConstants.ATTR_CONSOLE_ENCODING);
        if (encoding != null && encoding.trim().length() > 0) {
            arrayAsMapEnv.put("PYDEV_CONSOLE_ENCODING", encoding);

            //In Python 3.0, we can use the PYTHONIOENCODING.
            //Note that we always replace it, because this is really the encoding of the allocated console view,
            //so, if we had something different put, the encoding from the Python side would differ from the encoding
            //on the java side (which would make things garbled anyways).
            arrayAsMapEnv.put("PYTHONIOENCODING", encoding);
        }
        envp = ProcessUtils.getMapEnvAsArray(arrayAsMapEnv);
        Process p = DebugPlugin.exec(cmdLine, workingDirectory, envp);
        if (p == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not execute python process.",
                    null));
        }
        PythonRunnerCallbacks.afterCreatedProcess.call(p);
        return p;
    }

    /**
     * The debug plugin needs to be notified about our process.
     * It'll then display the appropriate UI.
     */
    private static IProcess registerWithDebugPluginForProcessType(String label, ILaunch launch, Process p,
            Map<String, String> processAttributes, PythonRunnerConfig config) {
        processAttributes.put(IProcess.ATTR_PROCESS_TYPE, config.getProcessType());
        processAttributes.put(IProcess.ATTR_PROCESS_LABEL, label);
        processAttributes.put(Constants.PYDEV_CONFIG_RUN, config.run);
        processAttributes.put(RelaunchConstants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR,
                RelaunchConstants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR_TRUE);
        processAttributes.put(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");

        ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
        boolean found = false;
        try {
            String attribute = launchConfiguration.getAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, (String) null);
            found = PyProcessFactory.PROCESS_FACTORY_ID.equals(attribute);
        } catch (CoreException e1) {
            Log.log(e1);
        }
        if (!found) {
            try {
                ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration.getWorkingCopy();
                workingCopy.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, PyProcessFactory.PROCESS_FACTORY_ID);
                workingCopy.doSave();
            } catch (CoreException e) {
                Log.log(e);
            }
        }

        return DebugPlugin.newProcess(launch, p, label, processAttributes);
    }

}
