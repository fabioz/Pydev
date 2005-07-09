/*
 * License: Common Public License v1.0
 * Created on Oct 25, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.service.environment.Constants;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

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
        
        if(osName.toLowerCase().indexOf("win") != -1){ //in windows, we have to put python "path_to_file.py"
            if(script.startsWith("\"") == false){
                script = "\""+script+"\"";
            }
        }

        String executionString = PydevPlugin.getInterpreterManager().getDefaultInterpreter() + " -u " + script + " " + args;
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
    public static String runAndGetOutputWithInterpreter(String interpreter, String script, String args, File workingDir, IProject project, IProgressMonitor monitor) {
        monitor.setTaskName("Mounting executable string...");
        monitor.worked(5);
        String osName = System.getProperty("os.name");
        
        if(osName.toLowerCase().indexOf("win") != -1){ //in windows, we have to put python "path_to_file.py"
            if(script.startsWith("\"") == false){
                script = "\""+script+"\"";
            }
        }

        String executionString = interpreter + " -u " + script + " " + args;
        monitor.worked(1);
        //System.out.println(executionString);
        return runAndGetOutput(executionString, workingDir, project, monitor);
    }

    public static String runAndGetOutput(String executionString, File workingDir, IProgressMonitor monitor) {
    	return runAndGetOutput(executionString, workingDir, null, monitor);
    }
    
    public static String runAndGetOutput(String executionString, File workingDir) {
    	return runAndGetOutput(executionString, workingDir, null, new NullProgressMonitor());
    }
    
    public static String runAndGetOutput(String executionString, File workingDir, IProject project) {
    	return runAndGetOutput(executionString, workingDir, project, new NullProgressMonitor());
    }

    /**
     * 
     * @param executionString
     * @return the process output.
     * @throws CoreException
     */
    public static String runAndGetOutput(String executionString, File workingDir, IProject project, IProgressMonitor monitor) {
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

        StringBuffer contents = new StringBuffer();
        if (process != null) {

            try {
                process.getOutputStream().close();
            } catch (IOException e2) {
            }

            monitor.setTaskName("Reading output...");
            monitor.worked(5);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()), BUFSIZE);


            
            try {
                int c;
                while ((c = in.read()) != -1) {
                    contents.append((char) c);
		            monitor.worked(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            
            try {
                monitor.setTaskName("Waiting for process to finish.");
                monitor.worked(5);
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
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     * 
     * changed so that we always set the PYTHONPATH in the environment
     * 
     * @param configuration
     * @return
     * @throws CoreException
     */
	public static String[] getEnvironment(IProject project) throws CoreException {
	    String pythonPathEnvStr = "";
		try {
	        if (PydevPlugin.getInterpreterManager().hasInfoOnDefaultInterpreter()){ //check if we have a default interpreter.
	            pythonPathEnvStr = makePythonPathEnvString(project);
	        }
        } catch (Exception e) {
            return null; //we cannot get it
        }

		DebugPlugin defaultPlugin = DebugPlugin.getDefault();
		if(defaultPlugin != null){
	        ILaunchManager launchManager = defaultPlugin.getLaunchManager();
	
		    // build base environment
			Map env = new HashMap();
			env.putAll(launchManager.getNativeEnvironment());
			
			// Add variables from config
			boolean win32= Platform.getOS().equals(Constants.OS_WIN32);
			for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry= (Map.Entry) iter.next();
				String key= (String) entry.getKey();
				if (win32) {
					// Win32 vars are case insensitive. Uppercase everything so
					// that (for example) "pAtH" will correctly replace "PATH"
					key= key.toUpperCase();
				}
				String value = (String) entry.getValue();
				// translate any string substitution variables
				String translated = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
				env.put(key, translated);
			}		
	
			env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
			return getMapEnvAsArray(env);
		}else{
		    return null;
		}
	}


    /**
     * @param project
     * @return
     */
    public static String makePythonPathEnvString(IProject project) {
        List paths;
        if(project != null){
            //if we have a project, get its complete pythonpath
	        IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
	    	paths = pythonPathNature.getCompleteProjectPythonPath();
        }else{
            //otherwise, get the system pythonpath
            InterpreterInfo info = PydevPlugin.getInterpreterManager().getDefaultInterpreterInfo(new NullProgressMonitor());
            paths = new ArrayList(info.libs);
        }

    	StringBuffer pythonpath = new StringBuffer();
        for (int i = 0; i < paths.size(); i++) {
    		if (i > 0){
    			pythonpath.append(";");
    		}
    		pythonpath.append(REF.getFileAbsolutePath(new File((String) paths.get(i))));
    	}
        return pythonpath.toString();
    }


    /**
     * @param env
     * @return
     */
    private static String[] getMapEnvAsArray(Map env) {
        List strings= new ArrayList(env.size());
		for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry) iter.next();
			StringBuffer buffer= new StringBuffer((String) entry.getKey());
			buffer.append('=').append((String) entry.getValue());
			strings.add(buffer.toString());
		}
		
		return (String[]) strings.toArray(new String[strings.size()]);
    }

    
}