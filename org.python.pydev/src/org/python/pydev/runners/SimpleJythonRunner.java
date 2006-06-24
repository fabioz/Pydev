/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class SimpleJythonRunner extends SimpleRunner{
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
    public Tuple<String,String> runAndGetOutput(String executionString, File workingDir, IProject project, IProgressMonitor monitor) {
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

            final String stdout = std.contents.toString();
            final String stderr = err.contents.toString();
            return new Tuple<String,String>(stdout, stderr);
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating python process - got null process("
                        + executionString + ")", new Exception("Error creating python process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return new Tuple<String,String>("", "Error creating python process - got null process("+ executionString + ")"); //no output
    }

    public Tuple<String,String> runAndGetOutputWithJar(String script, String jythonJar, String args, File workingDir, IProject project, IProgressMonitor monitor) {
        //"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" "-Dpython.home=C:\bin\jython21" 
        //-classpath "C:\bin\jython21\jython.jar;%CLASSPATH%" org.python.util.jython %ARGS%
        //used just for getting info without any classpath nor pythonpath
        
        try {
            String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getCanonicalPath();

            String[] s = new String[]{
                javaLoc,
                "-classpath",
                jythonJar,
                "org.python.util.jython" 
                ,script
            };
            String executionString = getCommandLineAsString(s);

            return runAndGetOutput(executionString, workingDir, project, monitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    @Override
    public Tuple<String,String> runAndGetOutput(String script, String[] args, File workingDir, IProject project) {
        //"java.exe" -classpath "C:\bin\jython21\jython.jar" -Dpython.path xxx;xxx;xxx org.python.util.jython script %ARGS%

        try {
            String executionString = makeExecutableCommandStr(script);
            
            return runAndGetOutput(executionString, workingDir, project);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

    /**
     * @param script
     * @return
     * @throws IOException
     */
    public static String makeExecutableCommandStr(String script) throws IOException {
        IInterpreterManager interpreterManager = PydevPlugin.getJythonInterpreterManager();
        String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getCanonicalPath();
        
        File file = new File(javaLoc);
        if(file.exists() == false ){
            throw new RuntimeException("The java location found does not exist. "+javaLoc);
        }
        if(file.isDirectory() == true){
            throw new RuntimeException("The java location found is a directory. "+javaLoc);
        }

        
        
        String jythonJar = interpreterManager.getDefaultInterpreter();
        InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(jythonJar, new NullProgressMonitor());

        StringBuffer jythonPath = new StringBuffer();
        String pathSeparator = SimpleRunner.getPythonPathSeparator();
        for (String lib : info.libs) {
            if(jythonPath.length() != 0){
                jythonPath.append(pathSeparator); 
            }
            jythonPath.append(lib);
        }
        
        
        String[] s = new String[]{
            javaLoc ,
            "-Dpython.path="+ jythonPath.toString(), 
            "-classpath",
            jythonJar+pathSeparator+jythonPath,
            "org.python.util.jython",
            script
        };
        String executionString = getCommandLineAsString(s);

        return executionString;
    }

}

