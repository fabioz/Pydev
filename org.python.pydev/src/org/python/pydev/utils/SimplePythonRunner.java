/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.PyProjectProperties;

/**
 * 
 * This class has some useful methods for running a python script.
 * 
 * It is not as complete as the PythonRunner from the debug, as it doesn't register the process in the console, but it can be quite useful
 * for other runs.
 * 
 * @author Fabio Zadrozny
 */
public class SimplePythonRunner {

    private static int BUFSIZE = 128;

    /**
     * Just execute the string. do nothing else...
     * @param executionString
     * @param workingDir
     * @return
     * @throws IOException
     */
    public static Process createProcess(String executionString, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(executionString, null, workingDir);
    }

    
    public static String runAndGetOutput(String script, String args, File workingDir) {
    	return runAndGetOutput(script, args, workingDir, null);
    }
    
    /**
     * Execute the string and format for windows if we have spaces...
     * 
     * @param script
     * @param args
     * @param workingDir
     * @return
     */
    public static String runAndGetOutput(String script, String args, File workingDir, IProject project) {
        String osName = System.getProperty("os.name");
        
        String execMsg;
        if(osName.toLowerCase().indexOf("win") != -1){ //in windows, we have to put python "path_to_file.py"
            if(script.startsWith("\"") == false){
                script = "\""+script+"\"";
            }
        }

        String executionString = PydevPrefs.getDefaultInterpreter() + " -u " + script + " " + args;
        //System.out.println(executionString);
        return runAndGetOutput(executionString, workingDir, project);
    }

    /**
     * Execute the string and format for windows if we have spaces...
     * 
     * @param script
     * @param args
     * @param workingDir
     * @return
     */
    public static String runAndGetOutputWithInterpreter(String interpreter, String script, String args, File workingDir, IProject project) {
        String osName = System.getProperty("os.name");
        
        String execMsg;
        if(osName.toLowerCase().indexOf("win") != -1){ //in windows, we have to put python "path_to_file.py"
            if(script.startsWith("\"") == false){
                script = "\""+script+"\"";
            }
        }

        String executionString = interpreter + " -u " + script + " " + args;
        //System.out.println(executionString);
        return runAndGetOutput(executionString, workingDir, project);
    }

    public static String runAndGetOutput(String executionString, File workingDir) {
    	return runAndGetOutput(executionString, workingDir, (List)null);
    }
    
    public static String runAndGetOutput(String executionString, File workingDir, IProject project) {
    	List paths = PyProjectProperties.getProjectPythonPath(project);
    	return runAndGetOutput(executionString, workingDir, paths);
    }

    /**
     * 
     * @param executionString
     * @return the process output.
     * @throws CoreException
     */
    private static String runAndGetOutput(String executionString, File workingDir, List paths) {
        Process process = null;
        try {
	    	String[] envp = getEnv(paths);
	        process = Runtime.getRuntime().exec(executionString, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StringBuffer contents = new StringBuffer();
        if (process != null) {

            try {
                process.getOutputStream().close();
            } catch (IOException e2) {
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()), BUFSIZE);


            
            try {
                int c;
                while ((c = in.read()) != -1) {
                    contents.append((char) c);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            
            try {
                process.waitFor(); //wait until the process completion.
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }

            
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating python process - got null process("
                        + executionString + ")", new Exception("Error creating python process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return contents.toString();
    }




    /**
     * @param envp
     * @param paths
     * @return
     */
    private static String[] getEnv(List paths) {
        String[] envp = null;
        if(paths != null){
	        int envSize = paths.size();
	        
	        if (envSize >= 1) {
	            Properties properties = System.getProperties();
	            String path = "PATH="+properties.get("java.library.path").toString();
	            String pythonpath = "PYTHONPATH=";

	            for (int i = 0; i < envSize; i++) {
	        		if (i > 0){
	        			pythonpath = pythonpath.concat(";");//XXX Need to be platform specific
	        		}
	        		
	        		pythonpath = pythonpath.concat(paths.get(i).toString());
	        	}
	        	
	        	
	        	envp = new String[2];
	        	envp[0] = pythonpath;
	        	envp[1] = path;
	        }
        }
        return envp;
    }
}