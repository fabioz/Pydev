/*
 * License: Common Public License v1.0
 * Created on Oct 25, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.Tuple;
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
    public Tuple<String,String> runAndGetOutputFromPythonScript(String script, String[] args, File workingDir, IProject project) {
    	String[] parameters = addInterpreterToArgs(script, args);
        return runAndGetOutput(parameters, workingDir, project, new NullProgressMonitor());
    }

    /**
     * @param script the script to run
     * @param args the arguments to be passed to the script
     * @return the string with the command to run the passed script with jython
     */
    public static String[] makeExecutableCommandStr(String script, String[] args) {
        String[] s = addInterpreterToArgs(script, args);
        
        List<String> asList = new ArrayList<String>(Arrays.asList(s));
        asList.addAll(Arrays.asList(args));

        return asList.toArray(new String[0]);
    }

	private static String[] addInterpreterToArgs(String script, String[] args) {
		String interpreter = PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
		return preparePythonCallParameters(interpreter, script, args);
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
    public Tuple<String,String> runAndGetOutputWithInterpreter(String interpreter, String script, String[] args, File workingDir, IProject project, IProgressMonitor monitor) {
        monitor.setTaskName("Mounting executable string...");
        monitor.worked(5);
        
        File file = new File(script);
        if(file.exists() == false){
            throw new RuntimeException("The script passed for execution ("+script+") does not exist.");
        }
        
        String[] s = preparePythonCallParameters(interpreter, script, args);
        monitor.worked(1);
        return runAndGetOutput(s, workingDir, project, monitor);
    }

    /**
     * Creates array with what should be passed to Runtime.exec to run python.
     * 
     * @param interpreter interpreter that should do the run
     * @param script python script to execute
     * @param args additional arguments to pass to python
     * @return the created array
     */
	private static String[] preparePythonCallParameters(String interpreter, String script, String[] args) {
		if (args == null) {
			args = new String[0];
		}
		
		String[] s = new String[3 + args.length];
        s[0] = interpreter;
        s[1] = "-u";
        s[2] = script;
        System.arraycopy(args, 0, s, 3, args.length);
		return s;
	}



    
}