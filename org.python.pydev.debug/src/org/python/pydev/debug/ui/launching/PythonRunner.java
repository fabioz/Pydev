/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

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
import org.python.pydev.runners.SimpleRunner;

/**
 * Launches Python process, and connects it to Eclipse's debugger.
 * Waits for process to complete.
 * 
 * Modelled after org.eclipse.jdt.internal.launching.StandardVMDebugger.
 */
public class PythonRunner {

    /**
     * @param p
     * @param process
     * @throws CoreException
     */
    private static void checkProcess(Process p, IProcess process) throws CoreException {
        if (process == null) {
            p.destroy();
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not register with debug plugin?", null));
        }
    }
    /**
     * @param p
     * @throws CoreException
     */
    private static void checkProcess(Process p) throws CoreException {
        if (p == null)
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,"Could not execute python process. Was it cancelled?", null));
    }

    
	/**
	 * Launches the configuration
     * 
     * The code is modeled after Ant launching example.
	 */
	public static void run(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
		if (config.isDebug) {
		    runDebug(config, launch, monitor);
            
		}else if (config.isUnittest()) { 
			runUnitTest(config, launch, monitor);
            
		}else { //default - just configured by command line (the others need special attention)
	        doIt(config, monitor, config.envp, config.getCommandLine(), config.workingDirectory, launch);
		}
	}

	/**
	 * Launches the config in the debug mode.
	 * 
	 * Loosely modeled upon Ant launcher.
	 */
	private static void runDebug(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
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
		checkProcess(p);
		
		IProcess process = registerWithDebugPlugin(config, launch, p);
        checkProcess(p, process);

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
									config.resource, debugger);
		launch.setSourceLocator(new PySourceLocator());
		debugger.startTransmission(); // this starts reading/writing from sockets
		t.initialize();
		t.addConsoleInputListener();
	}

    private static IProcess doIt(PythonRunnerConfig config, IProgressMonitor monitor, String [] envp, String[] cmdLine, File workingDirectory, ILaunch launch) throws CoreException{
        if (monitor == null)
        	monitor = new NullProgressMonitor();
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);

        subMonitor.beginTask("Launching python", 1);
        
        // Launch & connect to the debugger		
        subMonitor.subTask("Constructing command_line...");
        String commandLineAsString = SimpleRunner.getCommandLineAsString(cmdLine);
        System.out.println("running command line: "+commandLineAsString);
        Map processAttributes = new HashMap();
            
        processAttributes.put(IProcess.ATTR_CMDLINE, commandLineAsString);
        
        subMonitor.subTask("Exec...");
        
        //it was dying before register, so, I made this faster to see if this fixes it
        Process p = DebugPlugin.exec(cmdLine, workingDirectory, envp);	
        checkProcess(p);

        IProcess process;
        String label = cmdLine[cmdLine.length-1];
        if(config.isJython()) {
            if(config.isInteractive){
                label = "Interactive session: "+cmdLine[0]+" ... "+config.interpreter.toOSString()+" ("+config.resource.lastSegment()+")"; //java jython.jar
            }
            process = registerWithDebugPluginForProcessType(label, launch, p, processAttributes, "java");
        } else {
            if(config.isInteractive){
                label = "Interactive session: "+cmdLine[0]+" ("+config.resource.lastSegment()+")"; //c:/bin/python.exe
            }
            process = registerWithDebugPlugin(label, launch, p, processAttributes);
        }
        checkProcess(p, process);

        // Registered the process with the debug plugin
        subMonitor.subTask("Done");
        return process;
    }

    private static void runUnitTest(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException{
    	doIt(config, monitor, config.envp, config.getCommandLine(), config.workingDirectory, launch);
    }

    /**
	 * The debug plugin needs to be notified about our process.
	 * It'll then display the appropriate UI.
	 */
	private static IProcess registerWithDebugPlugin(PythonRunnerConfig config, ILaunch launch, Process p) {
		HashMap processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_CMDLINE, config.getCommandLineAsString());
		return registerWithDebugPlugin(config.resource.lastSegment(), launch,p, processAttributes);
	}

    /**
	 * The debug plugin needs to be notified about our process.
	 * It'll then display the appropriate UI.
	 */
    private static IProcess registerWithDebugPlugin(String cmdLine, String label, ILaunch launch, Process p) {
		HashMap processAttributes = new HashMap();
		processAttributes.put(IProcess.ATTR_CMDLINE, cmdLine);
		return registerWithDebugPlugin(label, launch,p, processAttributes);
	}
    
    /**
     * The debug plugin needs to be notified about our process.
     * It'll then display the appropriate UI.
     */
    private static IProcess registerWithDebugPlugin(String label, ILaunch launch, Process p, Map processAttributes) {
        processAttributes.put(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
        processAttributes.put(IProcess.ATTR_PROCESS_LABEL, label);
        processAttributes.put(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");
        return DebugPlugin.newProcess(launch,p, label, processAttributes);
    }
	
	/**
	 * The debug plugin needs to be notified about our process.
	 * It'll then display the appropriate UI.
	 */
    private static IProcess registerWithDebugPluginForProcessType(String label, ILaunch launch, Process p, Map processAttributes, String processType) {
	    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, processType);
	    processAttributes.put(IProcess.ATTR_PROCESS_LABEL, label);
        processAttributes.put(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");
	    return DebugPlugin.newProcess(launch,p, label, processAttributes);
	}
}
