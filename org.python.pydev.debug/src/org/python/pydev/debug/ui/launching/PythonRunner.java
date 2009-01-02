/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
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
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.RemoteDebugger;
import org.python.pydev.plugin.PydevPlugin;
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
    public static void run(final PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
        //let's check if the interpreter is valid.
        final IInterpreterManager interpreterManager = config.getRelatedInterpreterManager();
        if(!interpreterManager.hasInfoOnInterpreter(config.interpreterLocation)){
            final Display display = Display.getDefault();
            display.syncExec(new Runnable(){

                public void run() {
                    String msg = "The interpreter '%s' is not correctly configured as a '%s' interpreter.\n\n" +
                            "Reasons: If it is an old interpreter, you can open the run dialog (Menu: run > run) " +
                            "and choose a new interpreter in the arguments tab.\n\n" +
                            "Another reason could be that you're running some file from a jython project with a " +
                            "python interpreter (or vice-versa), so, you have to change the project type in the " +
                            "project properties.";
                    MessageDialog.openError(display.getActiveShell(), "Invalid Interpreter", 
                            StringUtils.format(msg, config.interpreterLocation, interpreterManager.getManagerRelatedName()));
                }
                
            });
            return;
        }
        
        try{
            if (config.isDebug) {
                runDebug(config, launch, monitor);
                
            }else if (config.isUnittest()) { 
                runUnitTest(config, launch, monitor);
                
            }else { //default - just configured by command line (the others need special attention)
                doIt(config, monitor, config.envp, config.getCommandLine(true), config.workingDirectory, launch);
            }
        }catch (final JDTNotAvailableException e) {
            PydevPlugin.log(e);
            final Display display = Display.getDefault();
            display.syncExec(new Runnable(){

                public void run() {
                    MessageDialog.openError(display.getActiveShell(), "Unable to run the selected configuration.", e.getMessage());
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
    private static void runDebug(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException, JDTNotAvailableException {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 5);
        subMonitor.beginTask("Launching python", 1);
        
        // Launch & connect to the debugger        
        RemoteDebugger debugger = new RemoteDebugger(config);
        debugger.startConnect(subMonitor);
        subMonitor.subTask("Constructing command_line...");
        String[] cmdLine = config.getCommandLine(true);

        Process p = createProcess(launch, config.envp, cmdLine, config.workingDirectory);
        checkProcess(p);
        
        IProcess process = registerWithDebugPlugin(config, launch, p);
        checkProcess(p, process);

        subMonitor.subTask("Waiting for connection...");
        Socket socket = null;
        try {
            socket = debugger.waitForConnect(subMonitor, p, process);
            if (socket == null) {
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
        PyDebugTarget t = new PyDebugTarget(launch, process, config.resource, debugger);
        launch.setSourceLocator(new PySourceLocator());
        t.startTransmission(socket); // this starts reading/writing from sockets
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
        String commandLineAsString = SimpleRunner.getArgumentsAsStr(cmdLine);
        //System.out.println("running command line: "+commandLineAsString);
        Map<Object, Object> processAttributes = new HashMap<Object, Object>();
            
        processAttributes.put(IProcess.ATTR_CMDLINE, commandLineAsString);
        
        subMonitor.subTask("Exec...");
        
        //it was dying before register, so, I made this faster to see if this fixes it
        Process p = createProcess(launch, envp, cmdLine, workingDirectory);    
        checkProcess(p);

        IProcess process;
        String label = cmdLine[cmdLine.length-1];
        if(config.isJython()) {
            if(config.isInteractive){
                throw new RuntimeException("Interactive not supported here!");
            }
            process = registerWithDebugPluginForProcessType(label, launch, p, processAttributes, "java");
        } else {
            
            //in the interactive session, we'll just create the process, it won't actually be registered
            //in the debug plugin (the communication is all done through xml-rpc).
            if(config.isInteractive){
                throw new RuntimeException("Interactive not supported here!");
            }
            process = registerWithDebugPlugin(label, launch, p, processAttributes);
        }
        checkProcess(p, process);

        // Registered the process with the debug plugin
        subMonitor.subTask("Done");
        return process;
    }
    
    /**
     * Actually creates the process (and create the encoding config file)
     */
    @SuppressWarnings("deprecation")
    private static Process createProcess(ILaunch launch, String[] envp, String[] cmdLine, File workingDirectory) throws CoreException {
        //Not using DebugPlugin.ATTR_CONSOLE_ENCODING to provide backward compatibility for eclipse 3.2
        String encoding = launch.getAttribute(IDebugUIConstants.ATTR_CONSOLE_ENCODING);
        if(encoding != null && encoding.trim().length() > 0){
            String[] s = new String[envp.length+2];
            System.arraycopy(envp, 0, s, 0, envp.length);
            s[s.length-2] = "PYDEV_CONSOLE_ENCODING="+encoding;
            //In Python 3.0, we can use the PYTHONIOENCODING.
            s[s.length-1] = "PYTHONIOENCODING="+encoding;
            envp = s;
        }        
        Process p = DebugPlugin.exec(cmdLine, workingDirectory, envp);
        return p;
    }

    private static void runUnitTest(PythonRunnerConfig config, ILaunch launch, IProgressMonitor monitor) throws CoreException, JDTNotAvailableException{
        doIt(config, monitor, config.envp, config.getCommandLine(true), config.workingDirectory, launch);
    }

    /**
     * The debug plugin needs to be notified about our process.
     * It'll then display the appropriate UI.
     * @throws JDTNotAvailableException 
     */
    private static IProcess registerWithDebugPlugin(PythonRunnerConfig config, ILaunch launch, Process p) throws JDTNotAvailableException {
        HashMap<Object, Object> processAttributes = new HashMap<Object, Object>();
        processAttributes.put(IProcess.ATTR_CMDLINE, config.getCommandLineAsString());
        return registerWithDebugPlugin(config.getRunningName(), launch,p, processAttributes);
    }

    
    /**
     * The debug plugin needs to be notified about our process.
     * It'll then display the appropriate UI.
     */
    private static IProcess registerWithDebugPlugin(String label, ILaunch launch, Process p, Map<Object, Object> processAttributes) {
        return registerWithDebugPluginForProcessType(label, launch, p, processAttributes, Constants.PROCESS_TYPE);
    }
    
    /**
     * The debug plugin needs to be notified about our process.
     * It'll then display the appropriate UI.
     */
    private static IProcess registerWithDebugPluginForProcessType(String label, ILaunch launch, Process p, 
            Map<Object, Object> processAttributes, String processType) {
        processAttributes.put(IProcess.ATTR_PROCESS_TYPE, processType);
        processAttributes.put(IProcess.ATTR_PROCESS_LABEL, label);
        processAttributes.put(DebugPlugin.ATTR_CAPTURE_OUTPUT, "true");
        
        return DebugPlugin.newProcess(launch,p, label, processAttributes);
    }
    
}
