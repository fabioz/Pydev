/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public abstract class SimpleRunner {

    /**
     * Passes the commands directly to Runtime.exec (with a null envp)
     */
    public static Process createProcess(String[] cmdarray, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(getWithoutEmptyParams(cmdarray), null, workingDir);
    }
    
    /**
     * Passes the commands directly to Runtime.exec (with a null envp)
     */
    public static Process createProcess(String[] cmdarray, String[] envp, File workingDir) throws IOException {
    	return Runtime.getRuntime().exec(getWithoutEmptyParams(cmdarray), getWithoutEmptyParams(envp), workingDir);
    }

    /**
     * @return a new array without any null/empty elements originally contained in the array.
     */
    private static String[] getWithoutEmptyParams(String[] cmdarray) {
    	if(cmdarray == null){
    		return null;
    	}
    	ArrayList<String> list = new ArrayList<String>();
    	for (String string : cmdarray) {
    		if(string != null && string.length() > 0){
    			list.add(string);
    		}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     * 
     * changed so that we always set the PYTHONPATH in the environment
     * 
     * @return the system environment with the PYTHONPATH env variable added for a given project (if it is null, return it with the
     * default PYTHONPATH added).
     */
    public String[] getEnvironment(IPythonNature pythonNature, String interpreter) throws CoreException {
        if(pythonNature == null){ //no associated nature in the project... just get the default env
            return getDefaultSystemEnvAsArray();
        }
        
        
        String pythonPathEnvStr = "";
    	try {
            
            if (PydevPlugin.getInterpreterManager(pythonNature).hasInfoOnInterpreter(interpreter)){ //check if we have a default interpreter.
                pythonPathEnvStr = makePythonPathEnvString(pythonNature, interpreter);
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null; //we cannot get it
        }
    
    	return createEnvWithPythonpath(pythonPathEnvStr);
    }

	public static String[] createEnvWithPythonpath(String pythonPathEnvStr) throws CoreException {
		DebugPlugin defaultPlugin = DebugPlugin.getDefault();
    	if(defaultPlugin != null){
            Map<String,String> env = getDefaultSystemEnv(defaultPlugin);		
    
    		env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
    		return getMapEnvAsArray(env);
    	}else{
    	    return null;
    	}
	}

    /**
     * @return an array with the env variables for the system with the format xx=yy  
     */
    public static String[] getDefaultSystemEnvAsArray() throws CoreException {
        Map<String,String> defaultSystemEnv = getDefaultSystemEnv();
        if(defaultSystemEnv != null){
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }

    /**
     * @return a map with the env variables for the system  
     */
    public static Map<String,String> getDefaultSystemEnv() throws CoreException {
        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        return getDefaultSystemEnv(defaultPlugin);
    }

    /**
     * @return a map with the env variables for the system  
     */
    @SuppressWarnings("unchecked")
    private static Map<String,String> getDefaultSystemEnv(DebugPlugin defaultPlugin) throws CoreException {
        if(defaultPlugin != null){
            ILaunchManager launchManager = defaultPlugin.getLaunchManager();
    
            // build base environment
            Map<String,String> env = new HashMap<String,String>();
            env.putAll(launchManager.getNativeEnvironment());
            
            // Add variables from config
            boolean win32= REF.isWindowsPlatform();
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
            	String translated = value;
            	try {
					translated = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value, false);
				} catch (Exception e) {
					Log.log(e);
				}
            	env.put(key, translated);
            }
            return env;
        }
        return null;
    }

    
    /**
     * copied from org.eclipse.jdt.internal.launching.StandardVMRunner
     * @param args - other arguments to be added to the command line (may be null)
     * @return
     */
    public static String getArgumentsAsStr(String[] commandLine, String ... args) {
        if(args != null && args.length > 0){
            String[] newCommandLine = new String[commandLine.length + args.length];
            System.arraycopy(commandLine, 0, newCommandLine, 0, commandLine.length);
            System.arraycopy(args, 0, newCommandLine, commandLine.length, args.length);
            commandLine = newCommandLine;
        }
        
        
        if (commandLine.length < 1)
            return ""; //$NON-NLS-1$
        FastStringBuffer buf= new FastStringBuffer();
        FastStringBuffer command= new FastStringBuffer();
        for (int i= 0; i < commandLine.length; i++) {
            if(commandLine[i] == null){
                continue; //ignore nulls (changed from original code)
            }
            
            buf.append(' ');
            char[] characters= commandLine[i].toCharArray();
            command.clear();
            boolean containsSpace= false;
            for (int j = 0; j < characters.length; j++) {
                char character= characters[j];
                if (character == '\"') {
                    command.append('\\');
                } else if (character == ' ') {
                    containsSpace = true;
                }
                command.append(character);
            }
            if (containsSpace) {
                buf.append('\"');
                buf.append(command.toString());
                buf.append('\"');
            } else {
                buf.append(command.toString());
            }
        }   
        return buf.toString();
    }   

    /**
     * Creates a string that can be passed as the PYTHONPATH 
     * 
     * @param project the project we want to get the settings from. If it is null, the system pythonpath is returned 
     * @param interpreter this is the interpreter to be used to create the env.
     * @return a string that can be used as the PYTHONPATH env variable
     */
    public static String makePythonPathEnvString(IPythonNature pythonNature, String interpreter) {
        if(pythonNature == null){
            return makePythonPathEnvFromPaths(new ArrayList<String>()); //no pythonpath can be gotten (set to empty, so that the default is gotten)
        }
        
        List<String> paths;
        
        //if we have a project, get its complete pythonpath
        IPythonPathNature pythonPathNature = pythonNature.getPythonPathNature();
        if(pythonPathNature == null){
            IProject project = pythonNature.getProject();
            String projectName;
            if(project == null){
                projectName = "null?";
            }else{
                projectName = project.getName();
            }
            throw new RuntimeException("The project "+projectName+" does not have the pythonpath configured, \n" +
                    "please configure it correcly (please check the pydev faq at \n" +
                    "http://pydev.sf.net/faq.html for better information on how to do it).");
        }
    	paths = pythonPathNature.getCompleteProjectPythonPath(interpreter);
    
        return makePythonPathEnvFromPaths(paths);
    }

    /**
     * @param paths the paths to be added
     * @return a String suitable to be added to the PYTHONPATH environment variable.
     */
	public static String makePythonPathEnvFromPaths(Collection<String> inPaths) {
	    ArrayList<String> paths = new ArrayList<String>(inPaths);
	    try {
	        //whenever we launch a file from pydev, we must add the sitecustomize to the pythonpath so that
	        //the default encoding (for the console) can be set.
	        //see: http://sourceforge.net/tracker/index.php?func=detail&aid=1580766&group_id=85796&atid=577329
	        
            paths.add(0, REF.getFileAbsolutePath(PydevPlugin.getScriptWithinPySrc("pydev_sitecustomize")));
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
	    
		String separator = getPythonPathSeparator();
    	StringBuffer pythonpath = new StringBuffer();
    	boolean first = true;
        for (String path:paths) {
    		if (first){
    			first = false;
    		}else{
    			pythonpath.append(separator);
    		}
    		
            pythonpath.append(path);
    	}
        return pythonpath.toString();
	}
    
    /**
     * @return the separator for the pythonpath variables (system dependent)
     */
    public static String getPythonPathSeparator(){
        return System.getProperty( "path.separator" ); //is system dependent, and should cover for all cases...
//        boolean win32= isWindowsPlatform();
//        String separator = ";";
//        if(!win32){
//            separator = ":"; //system dependent
//        }
//        return separator;
    }

    /**
     * @param env a map that will have its values formatted to xx=yy, so that it can be passed in an exec
     * @return an array with the formatted map
     */
    private static String[] getMapEnvAsArray(Map<String,String> env) {
        List<String> strings= new ArrayList<String>(env.size());
    	for(Iterator<Map.Entry<String, String>> iter= env.entrySet().iterator(); iter.hasNext(); ) {
    		Map.Entry<String, String> entry = iter.next();
    		StringBuffer buffer= new StringBuffer(entry.getKey());
    		buffer.append('=').append(entry.getValue());
    		strings.add(buffer.toString());
    	}
    	
    	return strings.toArray(new String[strings.size()]);
    }


    /**
     * Runs the given command line and returns a tuple with the output (stdout and stderr) of executing it.
     * 
     * @param cmdarray array with the commands to be passed to Runtime.exec
     * @param workingDir the working dir (may be null)
     * @param project the project (used to get the pythonpath and put it into the environment) -- if null, no environment is passed.
     * @param monitor the progress monitor to be used -- may be null
     * 
     * @return a tuple with stdout and stderr
     */
    public Tuple<String, String> runAndGetOutput(String[] cmdarray, File workingDir, IProject project, IProgressMonitor monitor) {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
    	String executionString = getArgumentsAsStr(cmdarray);
    	monitor.setTaskName("Executing: "+executionString);
        monitor.worked(5);
        Process process = null;
        try {
            monitor.setTaskName("Making pythonpath environment..."+executionString);
            String[] envp = getEnvironment(PythonNature.getPythonNature(project), null); //should get the environment for the default interpreter and the given project
            monitor.setTaskName("Making exec..."+executionString);
            if(workingDir != null){
                if(!workingDir.isDirectory()){
                    throw new RuntimeException(StringUtils.format("Working dir must be an existing directory (received: %s)", workingDir));
                }
            }
            process = createProcess(cmdarray, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getProcessOutput(process, executionString, monitor);
	}

    /**
     * @param process process from where the output should be gotten
     * @param executionString string to execute (only for errors)
     * @param monitor monitor for giving progress
     * @return a tuple with the output of stdout and stderr
     */
	protected Tuple<String, String> getProcessOutput(Process process,
			String executionString, IProgressMonitor monitor) {
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

            try {
                //just to see if we get something after the process finishes (and let the other threads run).
                synchronized (this) {
                    this.wait(50);
                }
            } catch (Exception e) {
                //ignore
            }
            return new Tuple<String, String>(std.contents.toString(), err.contents.toString());
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating process - got null process("
                        + executionString + ")", new Exception("Error creating process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return new Tuple<String, String>("","Error creating process - got null process("+ executionString + ")"); //no output
	}



    /**
     * @param pythonpath the pythonpath string to be used 
     * @return a list of strings with the elements of the pythonpath
     */
    public static List<String> splitPythonpath(String pythonpath) {
        ArrayList<String> splitted = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(pythonpath, getPythonPathSeparator());
        while(tokenizer.hasMoreTokens()){
            splitted.add(tokenizer.nextToken());
        }
        return splitted;
        
    }

}
