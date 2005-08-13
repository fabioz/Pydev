/*
 * License: Common Public License v1.0
 * Created on Oct 25, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.plugin.PydevPlugin;

/**
 * 
 * This class has some useful methods for running a python script.
 * 
 * It is not as complete as the PythonRunner from the debug, as it doesn't register the process in the console, but it can be quite useful
 * for other runs.
 * 
 * 
 * Interesting reading for http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html  -  
 * Navigate yourself around pitfalls related to the Runtime.exec() method
 * 
 * 
 * @author Fabio Zadrozny
 */
public class SimplePythonRunner extends SimpleRunner {

    
    /**
     * Execute the script specified with the interpreter for a given project 
     * 
     * @param script the script we will execute
     * @param args the arguments to pass to the script
     * @param workingDir the working directory
     * @param project the project that is associated to this run
     * 
     * @return a string with the output of the process (stdout)
     */
    public String runAndGetOutput(String script, String args, File workingDir, IProject project) {
        
        script = formatParamToExec(script);

        String executionString = makeExecutableCommandStr(script, args);
        //System.out.println(executionString);
        return runAndGetOutput(executionString, workingDir, project);
    }

    /**
     * @param script the script to run
     * @param args the arguments to be passed to the script
     * @return the string with the command to run the passed script with jython
     */
    public static String makeExecutableCommandStr(String script, String args) {
        script = formatParamToExec(script);

        return PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter() + " -u " + script + " " + args;
    }

    /**
     * Execute the string and format for windows if we have spaces...
     * 
     * The interpreter can be specified.
     * 
     * @param interpreter the interpreter we want to use for executing
     * @param script the python script to execute
     * @param args the arguments to the scripe
     * @param workingDir the directory where the script should be executed
     * 
     * @return the stdout of the run (if any)
     */
    public String runAndGetOutputWithInterpreter(String interpreter, String script, String args, File workingDir, IProject project, IProgressMonitor monitor) {
        monitor.setTaskName("Mounting executable string...");
        monitor.worked(5);
        
        script = formatParamToExec(script);

        String executionString;
        if(args != null){
            executionString = interpreter + " -u " + script + " " + args;
        }else{
            executionString = interpreter + " -u " + script;
        }
        monitor.worked(1);
        //System.out.println(executionString);
        return runAndGetOutput(executionString, workingDir, project, monitor);
    }



    /**
     * This is the method that actually does the running (all others are just 'shortcuts' to this one).
     * 
     * @param executionString this is the string that will be executed
     * @param workingDir this is the directory where the execution will happen
     * @param project this is the project that is related to the run (it is used to get the environment for the shell we are going to
     * execute with the correct pythonpath environment variable).
     * @param monitor this is the monitor used to communicate the progress to the user
     * 
     * @return the string that is the output of the process (stdout).
     */
    public String runAndGetOutput(String executionString, File workingDir, IProject project, IProgressMonitor monitor) {
        monitor.setTaskName("Executing: "+executionString);
        monitor.worked(5);
        Process process = null;
        try {
	        monitor.setTaskName("Making pythonpath environment...");
	    	String[] envp = getEnvironment(project);
	        monitor.setTaskName("Making exec.");
	        process = Runtime.getRuntime().exec(executionString, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (process != null) {

            try {
                process.getOutputStream().close(); //we won't write to it...
            } catch (IOException e2) {
            }

            monitor.setTaskName("Reading output...");
            monitor.worked(5);
            ThreadStreamReader std = new ThreadStreamReader(process.getInputStream());
            ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream());

            std.start();
            err.start();
            
            
            try {
                monitor.setTaskName("Waiting for process to finish.");
                monitor.worked(5);
                process.waitFor(); //wait until the process completion.
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }

            return std.contents.toString();
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating python process - got null process("
                        + executionString + ")", new Exception("Error creating python process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return ""; //no output
    }

    
}