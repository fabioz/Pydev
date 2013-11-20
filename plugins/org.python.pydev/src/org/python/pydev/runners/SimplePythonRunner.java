/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
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
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.Tuple;

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
    public Tuple<String, String> runAndGetOutputFromPythonScript(String interpreter, String script, String[] args,
            File workingDir, IProject project) {
        String[] parameters = addInterpreterToArgs(interpreter, script, args);
        return runAndGetOutput(parameters, workingDir, PythonNature.getPythonNature(project),
                new NullProgressMonitor(), null);
    }

    /**
     * @param script the script to run
     * @param args the arguments to be passed to the script
     * @return the string with the command to run the passed script with jython
     */
    public static String[] makeExecutableCommandStr(String interpreter, String script, String[] args) {
        String[] s = addInterpreterToArgs(interpreter, script, args);

        List<String> asList = new ArrayList<String>(Arrays.asList(s));
        asList.addAll(Arrays.asList(args));

        return asList.toArray(new String[0]);
    }

    private static String[] addInterpreterToArgs(String interpreter, String script, String[] args) {
        return preparePythonCallParameters(interpreter, script, args);
    }

    /**
     * Execute the string and format for windows if we have spaces...
     * 
     * The interpreter can be specified.
     * 
     * @param interpreter the interpreter we want to use for executing
     * @param script the python script to execute
     * @param args the arguments to the script
     * @param workingDir the directory where the script should be executed
     * 
     * @return the stdout of the run (if any)
     */
    public Tuple<String, String> runAndGetOutputWithInterpreter(String interpreter, String script, String[] args,
            File workingDir, IProject project, IProgressMonitor monitor, String encoding) {
        monitor.setTaskName("Mounting executable string...");
        monitor.worked(5);

        String[] s = preparePythonCallParameters(interpreter, script, args);
        monitor.worked(1);
        return runAndGetOutput(s, workingDir, PythonNature.getPythonNature(project), monitor, encoding);
    }

    /**
     * Creates array with what should be passed to Runtime.exec to run python.
     * 
     * @param interpreter interpreter that should do the run
     * @param script python script to execute
     * @param args additional arguments to pass to python
     * @return the created array
     */
    public static String[] preparePythonCallParameters(String interpreter, String script, String[] args) {
        File file = new File(script);
        if (file.exists() == false) {
            throw new RuntimeException("The script passed for execution (" + script + ") does not exist.");
        }

        //Note that we don't check it (interpreter could be just the string 'python')
        //        file = new File(interpreter);
        //        if(file.exists() == false){
        //            throw new RuntimeException("The interpreter passed for execution ("+interpreter+") does not exist.");
        //        }

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