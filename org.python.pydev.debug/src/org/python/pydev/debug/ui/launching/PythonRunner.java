/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.RemoteDebugger;
import org.python.pydev.debug.unittest.PyUnitTestRunner;
import org.python.pydev.debug.unittest.ITestRunListener;

/**
 * Launches Python process, and connects it to Eclipse's debugger.
 * Waits for process to complete.
 * 
 * Modelled after org.eclipse.jdt.internal.launching.StandardVMDebugger.
 */
public class PythonRunner {

	/**
	 * Launches the configuration
     * 
     * The code is modeled after Ant launching example.
	 */
	public static void run(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
		if (config.isDebug) {
		    runDebug(config, launch, monitor);
		}else if (config.isUnitTest) {
			runUnitTest(config, launch, monitor);
		}else { //default
	        doIt(monitor, config.envp, config.getCommandLine(), config.workingDirectory, launch);
		}
	}

	/**
	 * Launches the config in the debug mode.
	 * 
	 * Loosely modeled upon Ant launcher.
	 */
	public static void runDebug(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
		subMonitor.beginTask("Launching python", 1);
		
		// Launch & connect to the debugger		
		RemoteDebugger debugger = new RemoteDebugger(config);
		debugger.startConnect(subMonitor);
		subMonitor.subTask("Constructing command_line...");
		String[] cmdLine = config.getCommandLine();

		Process p = DebugPlugin.exec(cmdLine, config.workingDirectory, config.envp);	
		if (p == null)
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,"Could not execute python process. Was it cancelled?", null));
		
		IProcess process = registerWithDebugPlugin(config, launch, p);

		subMonitor.subTask("Waiting for connection...");
		try {
			boolean userCanceled = debugger.waitForConnect(subMonitor, p, process);
			if (userCanceled) {
				debugger.dispose();
				return;
			}
		}
		catch (Exception ex) {
			process.terminate();
			p.destroy();
			String message = "Unexpected error setting up the debugger";
			if (ex instanceof SocketTimeoutException)
				message = "Timed out after " + Float.toString(config.acceptTimeout/1000) + " seconds while waiting for python script to connect.";
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, message, ex));			
		}
		subMonitor.subTask("Done");
		// hook up debug model, and we are off & running
		PyDebugTarget t = new PyDebugTarget(launch, process, 
									config.file, debugger);
		launch.setSourceLocator(new PySourceLocator());
		debugger.startTransmission(); // this starts reading/writing from sockets
		t.initialize();
	}

    public static void doIt(IProgressMonitor monitor, String [] envp, String[] cmdLine, File workingDirectory, ILaunch launch) throws CoreException{
        if (monitor == null)
        	monitor = new NullProgressMonitor();
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
        subMonitor.beginTask("Launching python", 1);
        
        // Launch & connect to the debugger		
        subMonitor.subTask("Constructing command_line...");
        
        subMonitor.subTask("Exec...");
        
        Process p = DebugPlugin.exec(cmdLine, workingDirectory, envp);	
        if (p == null)
        	throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not execute python process. Was it cancelled?", null));

        // Register the process with the debug plugin
        subMonitor.subTask("Done");
        registerWithDebugPlugin(PythonRunnerConfig.getCommandLineAsString(cmdLine), cmdLine[cmdLine.length-1], launch, p);
        
    }

    public static void runUnitTest(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException{
        if (monitor == null)
        	monitor = new NullProgressMonitor();
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
        subMonitor.beginTask("Launching python in unittest mode", 1);
        
        // Launch & connect to the debugger		
        subMonitor.subTask("Constructing command_line...");
        subMonitor.subTask("Exec...");
		String[] cmdLine = config.getCommandLine();

        Process p = DebugPlugin.exec(cmdLine, config.workingDirectory, config.envp);	
        if (p == null)
        	throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not execute python process. Was it cancelled?", null));

        int port = config.getUnitTestPort();
        String full_path_to_file = config.file.toOSString();
        PyUnitTestRunner unitTestRunner = new PyUnitTestRunner(subMonitor, port, full_path_to_file);
		try {
			unitTestRunner.readTestResults();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
	        if(p != null){
	        	p.destroy();
	        }
		}
        
        // Register the process with the debug plugin
        subMonitor.subTask("Done");
        registerWithDebugPlugin(PythonRunnerConfig.getCommandLineAsString(cmdLine), cmdLine[cmdLine.length-1], launch, p);
        
    }

    /**
	 * The debug plugin needs to be notified about our process.
	 * It'll then display the appropriate UI.
	 */
	public static IProcess registerWithDebugPlugin(PythonRunnerConfig config, ILaunch launch, Process p) {
		HashMap processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
		processAttributes.put(IProcess.ATTR_CMDLINE, config.getCommandLineAsString());
		return DebugPlugin.newProcess(launch,p, config.file.lastSegment(), processAttributes);
	}

    /**
	 * The debug plugin needs to be notified about our process.
	 * It'll then display the appropriate UI.
	 */
	public static IProcess registerWithDebugPlugin(String cmdLine, String label, ILaunch launch, Process p) {
		HashMap processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
		processAttributes.put(IProcess.ATTR_CMDLINE, cmdLine);
		return DebugPlugin.newProcess(launch,p, label, processAttributes);
	}
}
