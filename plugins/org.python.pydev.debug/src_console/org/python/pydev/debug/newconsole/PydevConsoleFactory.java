/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.debug.model.remote.ListenConnector;
import org.python.pydev.debug.model.remote.RemoteDebuggerConsole;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.debug.newconsole.env.IProcessFactory.PydevConsoleLaunchInfo;
import org.python.pydev.debug.newconsole.env.JythonEclipseProcess;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.PydevPrefs;

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
        createConsole();
    }
    
    /**
     */
    public void createConsole() {
		createConsole(null);
	}
    
    /**
     * @return a new PydevConsole or null if unable to create it (user cancels it)
     */
    public void createConsole(String additionalInitialComands) {
        try {
            createConsole(createDefaultPydevInterpreter(), additionalInitialComands);
        } catch (Exception e) {
            Log.log(e);
        }
    }
    
	public void createConsole(final PydevConsoleInterpreter interpreter, final String additionalInitialComands) {
		Job job = new Job("Create Interactive Console") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Create Interactive Console", 10);
				try {
					ScriptConsoleManager manager = ScriptConsoleManager.getInstance();
					monitor.worked(1);
					if (interpreter != null) {
						final PydevConsole console = new PydevConsole(interpreter, additionalInitialComands);
						monitor.worked(1);
						createDebugTarget(interpreter, console, new SubProgressMonitor(monitor, 8));
						manager.add(console, true);
					}
					return Status.OK_STATUS;
				} catch (UserCanceledException uce) {
					return Status.CANCEL_STATUS;
				} catch (Exception e) {
					return PydevDebugPlugin
							.makeStatus(
									IStatus.ERROR,
									"Unable to connect debugger to Interactive Console\n"
											+ "The interactive console will continue to operate without the additional debugger features",
									e);
				} finally {
					monitor.done();
				}
			}

		};
		job.setUser(true);
		job.schedule();
	}

	private void createDebugTarget(PydevConsoleInterpreter interpreter, PydevConsole console, IProgressMonitor monitor) throws IOException, CoreException, DebugException, UserCanceledException {
		monitor.beginTask("Connect Debug Target", 2);
		try {
			// Jython within Eclipse does not yet support these new features
			Process process = interpreter.getProcess();
			if (InteractiveConsolePrefs.getConsoleConnectVariableView() && !(process instanceof JythonEclipseProcess)) {
				PyDebugTargetConsole pyDebugTargetConsole = null;
				PydevConsoleCommunication consoleCommunication = (PydevConsoleCommunication) interpreter.getConsoleCommunication();
				IProcess eclipseProcess = interpreter.getLaunch().getProcesses()[0];
				RemoteDebuggerConsole debugger = new RemoteDebuggerConsole();
				int acceptTimeout = PydevPrefs.getPreferences().getInt(PydevEditorPrefs.CONNECT_TIMEOUT);
				ListenConnector connector = new ListenConnector(acceptTimeout);
				debugger.startConnect(connector);
				pyDebugTargetConsole = new PyDebugTargetConsole(consoleCommunication, interpreter.getLaunch(),
						eclipseProcess, debugger);
	
				Socket socket = null;
				try {
					consoleCommunication.hello(new SubProgressMonitor(monitor, 1));
					consoleCommunication.connectToDebugger(connector.getLocalPort());
					socket = debugger.waitForConnect(monitor, process, eclipseProcess);
					if (socket == null) {
						throw new UserCanceledException("Cancelled");
					}
				} catch (Exception ex) {
					try {
						consoleCommunication.close();
						debugger.dispose();
					} catch (Exception e) {
						// Don't hide the more important exception from user
						Log.log(e);
					}
					if (ex instanceof UserCanceledException) {
						UserCanceledException userCancelled = (UserCanceledException)ex;
						throw userCancelled;
					}
			        String message = "Unexpected error setting up the debugger";
			        if (ex instanceof SocketTimeoutException)
			            message = "Timed out after " + Float.toString(acceptTimeout/1000) + " seconds while waiting for python script to connect.";
			        throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, message, ex));            
			    }
	
				pyDebugTargetConsole.startTransmission(socket); // this starts reading/writing from sockets
				pyDebugTargetConsole.initialize();
			    
				consoleCommunication.setDebugTarget(pyDebugTargetConsole);
				interpreter.getLaunch().addDebugTarget(pyDebugTargetConsole);
				ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
				launchManager.addLaunch(interpreter.getLaunch());
	
				pyDebugTargetConsole.setConsole(console);
				console.setProcess(pyDebugTargetConsole.getProcess());
				pyDebugTargetConsole.finishedInit = true;
			}
		} finally {
			monitor.done();
		}
	}

    /**
     * @return A PydevConsoleInterpreter with its communication configured.
     * 
     * @throws CoreException
     * @throws IOException
     * @throws UserCanceledException
     */
    public static PydevConsoleInterpreter createDefaultPydevInterpreter() throws Exception, 
            UserCanceledException {

//            import sys; sys.ps1=''; sys.ps2=''
//            import sys;print >> sys.stderr, ' '.join([sys.executable, sys.platform, sys.version])
//            print >> sys.stderr, 'PYTHONPATH:'
//            for p in sys.path:
//                print >> sys.stderr,  p
//
//            print >> sys.stderr, 'Ok, all set up... Enjoy'
        
        IProcessFactory iprocessFactory = new IProcessFactory();
        
        PydevConsoleLaunchInfo launchAndProcess = iprocessFactory.createInteractiveLaunch();
        if(launchAndProcess == null){
            return null;
        }
        return createPydevInterpreter(launchAndProcess, iprocessFactory.getNaturesUsed());

        
    }
    
    // Use IProcessFactory to get the required tuple
    public static PydevConsoleInterpreter createPydevInterpreter(
            PydevConsoleLaunchInfo info, List<IPythonNature> natures) throws Exception {
        final ILaunch launch = info.launch;
        Process process = info.process;
        Integer clientPort = info.clientPort;
        IInterpreterInfo interpreterInfo = info.interpreter;
        if(launch == null){
            return null;
        }

        PydevConsoleInterpreter consoleInterpreter = new PydevConsoleInterpreter();
        int port = Integer.parseInt(launch.getAttribute(IProcessFactory.INTERACTIVE_LAUNCH_PORT));
        consoleInterpreter.setConsoleCommunication(
        		new PydevConsoleCommunication(port, process, clientPort));
        consoleInterpreter.setNaturesUsed(natures);
        consoleInterpreter.setInterpreterInfo(interpreterInfo);
        consoleInterpreter.setLaunch(launch);
        consoleInterpreter.setProcess(process);
        
        PydevDebugPlugin.getDefault().addConsoleLaunch(launch);
        
        consoleInterpreter.addCloseOperation(new Runnable() {
            public void run() {
                PydevDebugPlugin.getDefault().removeConsoleLaunch(launch);
            }
        });
        return consoleInterpreter;

    	
    }

}
