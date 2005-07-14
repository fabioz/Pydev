/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.core.REF;
import org.python.pydev.debug.codecoverage.PyCoverage;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.utils.SimplePythonRunner;

/**
 * Holds configuration for PythonRunner.
 * 
 * It knows how to extract proper launching arguments from disparate sources. 
 * Has many launch utility functions (getCommandLine & friends).
 */
public class PythonRunnerConfig {

    public static final String RUN_COVERAGE = "RUN_COVERAGE";
    public static final String RUN_REGULAR = "RUN_REGULAR";
    public static final String RUN_UNITTEST = "RUN_UNITTEST";
    
    
	public IPath resource;
	public String interpreter;
	public String[] arguments;
	public File workingDirectory;
	// debugging
	public boolean isDebug;
	private int debugPort = 0;  // use getDebugPort
	public int acceptTimeout = 5000; // miliseconds
	public String[] envp = null;

	// unit test specific
	private int unitTestPort = 0;  // use getUnitTestPort
	private String unitTestModule;
	private String unitTestModuleDir;
    private String run;
    private ILaunchConfiguration configuration;

    public boolean isCoverage(){
        return this.run.equals(RUN_COVERAGE);
    }
    
    public boolean isUnittest(){
        return this.run.equals(RUN_UNITTEST);
    }
    
    public boolean isFile() throws CoreException{
        int resourceType = configuration.getAttribute(Constants.ATTR_RESOURCE_TYPE, -1);
        return resourceType == IResource.FILE;
    }
    
    
    /**
     * Expands and returns the location attribute of the given launch
     * configuration. The location is
     * verified to point to an existing file, in the local file system.
     * 
     * @param configuration launch configuration
     * @return an absolute path to a file in the local file system  
     * @throws CoreException if unable to retrieve the associated launch
     * configuration attribute, if unable to resolve any variables, or if the
     * resolved location does not point to an existing file in the local file
     * system
     */
    public static IPath getLocation(ILaunchConfiguration configuration) throws CoreException {
        String location = configuration.getAttribute(Constants.ATTR_LOCATION, (String) null);
        if (location == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get location for run", null));
        } else {
            String expandedLocation = getStringVariableManager().performStringSubstitution(location);
            if (expandedLocation == null || expandedLocation.length() == 0) {
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get expanded location for run", null));
            } else {
                return new Path(expandedLocation);
            }
        }
    }
    
    /**
     * Expands and returns the arguments attribute of the given launch
     * configuration. Returns <code>null</code> if arguments are not specified.
     * 
     * @param configuration launch configuration
     * @return an array of resolved arguments, or <code>null</code> if
     * unspecified
     * @throws CoreException if unable to retrieve the associated launch
     * configuration attribute, or if unable to resolve any variables
     */
    public static String[] getArguments(ILaunchConfiguration configuration) throws CoreException {
        String args = configuration.getAttribute(Constants.ATTR_TOOL_ARGUMENTS, (String) null);
        if (args != null) {
            String expanded = getStringVariableManager().performStringSubstitution(args);
            return parseStringIntoList(expanded);
        }
        return null;
    }
    /**
     * Parses the argument text into an array of individual
     * strings using the space character as the delimiter.
     * An individual argument containing spaces must have a
     * double quote (") at the start and end. Two double 
     * quotes together is taken to mean an embedded double
     * quote in the argument text.
     * 
     * @param arguments the arguments as one string
     * @return the array of arguments
     */
    public static String[] parseStringIntoList(String arguments) {
        if (arguments == null || arguments.length() == 0) {
            return new String[0];
        }
        String[] res= DebugPlugin.parseArguments(arguments);
        return res;     
    }   

    
    private static IStringVariableManager getStringVariableManager() {
        return VariablesPlugin.getDefault().getStringVariableManager();
    }
    /**
     * Expands and returns the working directory attribute of the given launch
     * configuration. Returns <code>null</code> if a working directory is not
     * specified. If specified, the working is verified to point to an existing
     * directory in the local file system.
     * 
     * @param configuration launch configuration
     * @return an absolute path to a directory in the local file system, or
     * <code>null</code> if unspecified
     * @throws CoreException if unable to retrieve the associated launch
     * configuration attribute, if unable to resolve any variables, or if the
     * resolved location does not point to an existing directory in the local
     * file system
     */
    public static IPath getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
        String location = configuration.getAttribute(Constants.ATTR_WORKING_DIRECTORY, (String) null);
        if (location != null) {
            String expandedLocation = getStringVariableManager().performStringSubstitution(location);
            if (expandedLocation.length() > 0) {
                File path = new File(expandedLocation);
                if (path.isDirectory()) {
                    return new Path(expandedLocation);
                } 
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get working location for the run",null));
            }
        }
        return null;
    }


	/**
	 * Sets defaults.
	 */
	public PythonRunnerConfig(ILaunchConfiguration conf, String mode, String run) throws CoreException {
	    this.configuration = conf;
        this.run = run;
		isDebug = mode.equals(ILaunchManager.DEBUG_MODE);
		
		resource = getLocation(conf);
		interpreter = conf.getAttribute(Constants.ATTR_INTERPRETER, "python");
		arguments = getArguments(conf);
		IPath workingPath = getWorkingDirectory(conf);
		workingDirectory = workingPath == null ? null : workingPath.toFile();
		acceptTimeout = PydevPrefs.getPreferences().getInt(PydevPrefs.CONNECT_TIMEOUT);

        if (isUnittest()){
			setUnitTestInfo();
		}

		//find the project
        IWorkspace w = ResourcesPlugin.getWorkspace();
        String projName = conf.getAttribute(Constants.ATTR_PROJECT, "");
        if (projName.length() == 0){
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get project for the run",null));
        }
        
        IProject project = w.getRoot().getProject(projName);
        

        if(project == null){ //Ok, we could not find it out
            CoreException e = PydevPlugin.log("Could not get project for resource: "+resource);
            throw e;
        }
        
        //make the environment
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        envp = launchManager.getEnvironment(conf);
        if(envp == null){
            //ok, the user has done nothing to the environment, just get all the default environment and
            //put the pythonpath in it
            envp = SimplePythonRunner.getEnvironment(project);
        }else{
    		boolean win32= Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32);

    		//ok, the user has done something to configure it, so, just add the pythonpath to the
            //current env (if he still didn't do so)
    		Map envMap = conf.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map)null);

    		if(!specifiedPythonpath(envMap)){
	    		
	            String pythonpath = SimplePythonRunner.makePythonPathEnvString(project);
	            //override it if it was the ambient pythonpath
	            for (int i = 0; i < envp.length; i++) {
	                if(win32){
		                if(envp[i].toUpperCase().startsWith("PYTHONPATH")){
		                    //OK, finish it.
				            envp[i] = "PYTHONPATH="+pythonpath;
		                    return;
		                }
	                }else{
		                if(envp[i].startsWith("PYTHONPATH")){
		                    //OK, finish it.
				            envp[i] = "PYTHONPATH="+pythonpath;
		                    return;
		                }
	                }
	                
	            }
	            
	            //there was no pythonpath, let's set it
	            String[] s = new String[envp.length+1];
	            System.arraycopy(envp, 0, s, 0, envp.length);
	            s[s.length-1] = "PYTHONPATH="+pythonpath;
		            
    		}
        }
	}
	
    /**
     * @param envMap
     * @return
     */
    private boolean specifiedPythonpath(Map envMap) {
        if(envMap == null){
            return false;
            
        }else{
    		boolean win32= Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32);

            for (Iterator iter = envMap.keySet().iterator(); iter.hasNext();) {
                String s = (String) iter.next();

                if(win32){
                    if(s.toUpperCase().equals("PYTHONPATH")){
                        return true;
                    }
                }else{
                    if(s.equals("PYTHONPATH")){
                        return true;
                    }
                }
                
            }
        }
        
        return false;
    }

    public int getDebugPort() throws CoreException {
		if (debugPort == 0) {
			debugPort= SocketUtil.findUnusedLocalPort("", 5000, 15000); //$NON-NLS-1$
			if (debugPort == -1)
				throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not find a free socket for debugger", null));
		}
		return debugPort;		
	}

    public int getUnitTestPort(){
		return unitTestPort;		
	}

    public void setUnitTestPort() throws CoreException {
		unitTestPort = SocketUtil.findUnusedLocalPort("", 5000, 15000); //$NON-NLS-1$
		if (unitTestPort == -1)
			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not find a free socket for unit test run", null));
	}

    private void setUnitTestInfo() throws CoreException {
		setUnitTestPort();

    	// get the test module name and path so that we can import it in Python
		int segmentCount = resource.segmentCount();

		IPath noextPath = resource.removeFileExtension();
		unitTestModule =  noextPath.lastSegment();
		IPath modulePath = resource.uptoSegment(segmentCount-1);
		unitTestModuleDir = modulePath.toString();
    }
    
	public String getRunningName() {
		return resource.lastSegment();
	}

	/**
	 * @throws CoreException if arguments are inconsistent
	 */
	public void verify() throws CoreException {
		if (resource == null || interpreter == null){
		    throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Invalid PythonRunnerConfig",null));
        }
        
		if (isDebug && ( acceptTimeout < 0|| debugPort < 0) ){
		    throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Invalid PythonRunnerConfig",null));
        }
	}

	/**
     * @return
	 * @throws CoreException
     */
    public static String getCoverageScript() throws CoreException {
        return REF.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("coverage.py"));
    }

    public static String getUnitTestScript() throws CoreException {
        return REF.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("SocketTestRunner.py"));
    }

    /** 
	 * gets location of jpydaemon.py
	 */
	public static String getDebugScript() throws CoreException {
	    return REF.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("pydevd.py"));
	}

    private String getRunFilesScript() throws CoreException {
        return REF.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("runfiles.py"));
    }

	/**
	 * Create a command line for launching.
	 * @return command line ready to be exec'd
	 * @throws CoreException 
	 */
	public String[] getCommandLine() throws CoreException {
		Vector cmdArgs = new Vector(10);
		cmdArgs.add(interpreter);
		// Next option is for unbuffered stdout, otherwise Eclipse will not see any output until done
		cmdArgs.add(org.python.pydev.ui.pythonpathconf.InterpreterEditor.isJython(interpreter) ? "-i" : "-u");
		if (isDebug) {
			cmdArgs.add(getDebugScript());
			cmdArgs.add("--client");
			cmdArgs.add("localhost");
			cmdArgs.add("--port");
			cmdArgs.add(Integer.toString(debugPort));
			cmdArgs.add("--file");
		}
		
		if(isCoverage()){
			cmdArgs.add(getCoverageScript());
			String coverageFileLocation = PyCoverage.getCoverageFileLocation();
            System.out.println("coverageFileLocation "+coverageFileLocation);
            cmdArgs.add(coverageFileLocation);
			cmdArgs.add("-x");
			if (!isFile()){
			    //run all testcases
                cmdArgs.add(getRunFilesScript());
            }
		}

		if(isUnittest()){
            if (isFile()){
    			cmdArgs.add(getUnitTestScript());
    			cmdArgs.add(Integer.toString(getUnitTestPort()));
    			cmdArgs.add(unitTestModuleDir);
    			cmdArgs.add(unitTestModule);
    			String[] retVal = new String[cmdArgs.size()];
    			cmdArgs.toArray(retVal);
    			return retVal;

            }else{ //run all testcases
                cmdArgs.add(getRunFilesScript());
            }
		}

		cmdArgs.add(resource.toOSString());
		for (int i=0; arguments != null && i<arguments.length; i++){
			cmdArgs.add(arguments[i]);
        }
        
		String[] retVal = new String[cmdArgs.size()];
		cmdArgs.toArray(retVal);
		return retVal;
	}

	
	
	public String getCommandLineAsString() {
		String[] args;
        try {
            args = getCommandLine();
            return getCommandLineAsString(args);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
	}


    /**
     * @param args
     * @return
     */
    public static String getCommandLineAsString(String[] args) {
        StringBuffer s = new StringBuffer();
		for (int i=0; i< args.length; i++) {
			s.append(args[i]);
			s.append(" ");
		}
		return s.toString();
    }
}
