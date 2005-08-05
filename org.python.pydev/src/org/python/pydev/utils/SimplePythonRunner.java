/*
 * License: Common Public License v1.0
 * Created on Oct 25, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.File;
import java.io.IOException;
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
import org.python.pydev.core.REF;
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
 * 
 * Interesting reading for http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html  -  
 * Navigate yourself around pitfalls related to the Runtime.exec() method
 * 
 * 
 * @author Fabio Zadrozny
 */
public class SimplePythonRunner {


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
        if(project == null){ //no associated project... just get the default env
            return getDefaultSystemEnvAsArray();
        }
        
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
	        Map<String,String> env = getDefaultSystemEnv(defaultPlugin);		
	
			env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
			return getMapEnvAsArray(env);
		}else{
		    return null;
		}
	}

    
	public static String[] getDefaultSystemEnvAsArray() throws CoreException {
        Map defaultSystemEnv = getDefaultSystemEnv();
        if(defaultSystemEnv != null){
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }
    
	public static Map getDefaultSystemEnv() throws CoreException {
	    DebugPlugin defaultPlugin = DebugPlugin.getDefault();
	    return getDefaultSystemEnv(defaultPlugin);
    }

    /**
     * @param defaultPlugin
     * @return
     * @throws CoreException
     */
    public static Map<String,String> getDefaultSystemEnv(DebugPlugin defaultPlugin) throws CoreException {
        if(defaultPlugin != null){
            ILaunchManager launchManager = defaultPlugin.getLaunchManager();
    
            // build base environment
            Map<String,String> env = new HashMap<String,String>();
            env.putAll(launchManager.getNativeEnvironment());
            
            // Add variables from config
            boolean win32= isWindowsPlatform();
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
            return env;
        }
        return null;
    }


    /**
     * @return whether we are in windows or not
     */
    public static boolean isWindowsPlatform() {
        return Platform.getOS().equals(Constants.OS_WIN32);
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

        boolean win32= isWindowsPlatform();
        String separator = ";";
        if(!win32){
            separator = ":"; //system dependent
        }

    	StringBuffer pythonpath = new StringBuffer();
        for (int i = 0; i < paths.size(); i++) {
    		if (i > 0){
    			pythonpath.append(separator);
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
        List<String> strings= new ArrayList<String>(env.size());
		for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry) iter.next();
			StringBuffer buffer= new StringBuffer((String) entry.getKey());
			buffer.append('=').append((String) entry.getValue());
			strings.add(buffer.toString());
		}
		
		return strings.toArray(new String[strings.size()]);
    }

    
}