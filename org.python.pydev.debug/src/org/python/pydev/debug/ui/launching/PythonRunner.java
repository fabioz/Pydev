/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PythonDebugTarget;
import org.python.pydev.debug.model.RemoteDebugger;

/**
 * Launches Python process, and connects it to Eclipse's debugger.
 * Waits for process to complete.
 * 
 * Modelled after org.eclipse.jdt.internal.launching.StandardVMDebugger.
 */
public class PythonRunner {

	class DebugConnector implements Runnable {
		int port;
		int timeout;
		ServerSocket serverSocket;
		Socket socket;	 // what got accepted
		Exception e;

		boolean terminated;
		
		public DebugConnector(int port, int timeout) throws IOException {
			this.port = port;
			this.timeout = timeout;
			serverSocket = new ServerSocket(port);
		}
		
		Exception getException() {
			return e;
		}
	
		public Socket getSocket() {
			return socket;
		}

		public void stopListening() throws IOException {
			if (serverSocket != null)
				serverSocket.close();
			terminated = true;
		}
	
		public void run() {
			try {
				serverSocket.setSoTimeout(timeout);
				socket = serverSocket.accept();
			}
			catch (IOException e) {
				this.e = e;
			}
		}
	}

	/**
	 * launches the debug configuration
	 * @param config
	 * @param launch
	 * @param monitor
	 * @throws CoreException
	 */
	public void run(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
		subMonitor.beginTask("Launching python", 1);
		
		// Launch & connect to the debugger		
		subMonitor.subTask("Finding free socket...");
		DebugConnector server = new DebugConnector(config.getDebugPort(), config.acceptTimeout);
		subMonitor.worked(1);		
		subMonitor.subTask("Constructing command_line...");
		String[] cmdLine = config.getCommandLine();
		subMonitor.worked(1);
				
		Thread connectThread = new Thread(server, "Pydev debug listener");
		connectThread.start();
		Process p = DebugPlugin.exec(cmdLine, config.workingDirectory);	
		if (p == null)
			// TODO this might not be an error
			throw new CoreException(new Status(IStatus.ERROR, PydevDebugPlugin.getPluginID(), 0, "Could not execute python process. Was it cancelled?", null));

		// Register the process with the debug plugin
		subMonitor.worked(2);
		subMonitor.subTask("Starting debugger...");
		HashMap processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
		processAttributes.put(IProcess.ATTR_CMDLINE, config.getCommandLineAsString());
		IProcess process = DebugPlugin.newProcess(launch,p, config.file.lastSegment(), processAttributes);

		// Launch the debug listener on a thread, and wait until it completes
		while (connectThread.isAlive()) {
			if (monitor.isCanceled()) {
				server.stopListening();
				p.destroy();
				return;
			}
			try {
				p.exitValue(); // throws exception if process has terminated
				// process has terminated - stop waiting for a connection
				try {
					server.stopListening(); 
				} catch (IOException e) {
					// expected
				}
				checkErrorMessage(process);
			} catch (IllegalThreadStateException e) {
				// expected while process is alive
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		Exception ex = server.getException();
		if (ex != null) {
			process.terminate();
			p.destroy();
			String message = "Unexpected error setting up the debugger";
			if (ex instanceof SocketTimeoutException)
				message = "Timed out after " + Float.toString(config.acceptTimeout/1000) + " seconds while waiting for python script to connect.";
			throw new CoreException(new Status(IStatus.ERROR, PydevDebugPlugin.getPluginID(), 0, message, ex));
		}
		// hook up debug model, and we are off & running
		RemoteDebugger debugger = new RemoteDebugger(server.getSocket());
		PythonDebugTarget t = new PythonDebugTarget(launch, process, 
									config.getRunningName(), debugger);
		Thread dt = new Thread(debugger, "Pydev remote debug connection");
		dt.start();
	}
	
	protected void checkErrorMessage(IProcess process) throws CoreException {
		String errorMessage= process.getStreamsProxy().getErrorStreamMonitor().getContents();
		if (errorMessage.length() != 0)
			// TODO not sure if this is really an error
			throw new CoreException(new Status(IStatus.ERROR, PydevDebugPlugin.getPluginID(), 0, "Something got printed in the error stream", null));
	}
}
