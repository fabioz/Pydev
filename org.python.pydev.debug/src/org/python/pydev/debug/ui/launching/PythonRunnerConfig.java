/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.debug.codecoverage.PyCoverage;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.plugin.PyunitPrefsPage;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Holds configuration for PythonRunner.
 * 
 * It knows how to extract proper launching arguments from disparate sources. 
 * Has many launch utility functions (getCommandLine & friends).
 */
public class PythonRunnerConfig {

    public static final String RUN_COVERAGE = "python code coverage run";
    public static final String RUN_REGULAR  = "python regular run";
    public static final String RUN_UNITTEST = "pyton unittest run";
    public static final String RUN_JYTHON_UNITTEST = "jython unittest run";
    public static final String RUN_JYTHON   = "jython regular run";
        
    public IProject project;
	public IPath[] resource;
	public IPath interpreter;
	public String interpreterLocation;
	private String arguments;
	public File workingDirectory;
	public String pythonpathUsed;
	// debugging
	public boolean isDebug;
	public boolean isInteractive;
	private int debugPort = 0;  // use getDebugPort
	public int acceptTimeout = 5000; // miliseconds
	public String[] envp = null;

    private String run;
    private ILaunchConfiguration configuration;

    public boolean isCoverage(){
        return this.run.equals(RUN_COVERAGE);
    }
    
    public boolean isUnittest(){
        return this.run.equals(RUN_UNITTEST) || this.run.equals(RUN_JYTHON_UNITTEST);
    }
    
    public boolean isJython(){
        return this.run.equals(RUN_JYTHON) || this.run.equals(RUN_JYTHON_UNITTEST);
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
    public static IPath[] getLocation(ILaunchConfiguration configuration) throws CoreException {
        String locationsStr = configuration.getAttribute(Constants.ATTR_LOCATION, (String) null);
        
        if (locationsStr == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get location for run", null));
        } else {
        	String[] locations = StringUtils.split(locationsStr, '|');
        	Path[] ret = new Path[locations.length];
        	int i=0;
        	for(String location:locations){
	            String expandedLocation = getStringVariableManager().performStringSubstitution(location);
	            if (expandedLocation == null || expandedLocation.length() == 0) {
	                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get expanded location for run", null));
	            } else {
	                ret[i] = new Path(expandedLocation);
	            }
	            i++;
        	}
            return ret;
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
    public static String getArguments(ILaunchConfiguration configuration, boolean makeArgumentsVariableSubstitution) throws CoreException {
        String arguments = configuration.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "");
        if(makeArgumentsVariableSubstitution){
    		return VariablesPlugin.getDefault().getStringVariableManager()
    				.performStringSubstitution(arguments);
        }else{
            return arguments;
        }
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
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get working location for the run \n(the location: '"+expandedLocation+"' is not a valid directory).",null));
            }
        }
        return null;
    }

    /**
     * Returns the location of the selected interpreter in the launch configuration
     * @param conf
     * @return the string location of the selected interpreter in the launch configuration
     * @throws CoreException if unable to retrieve the launch configuration attribute or if unable to 
     * resolve the default interpreter.
     */
    public static String getInterpreterLocation(ILaunchConfiguration conf, IPythonNature nature) throws InvalidRunException, CoreException {
		IInterpreterManager interpreterManager = PydevPlugin.getInterpreterManager(nature);
        String location = conf.getAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);
        
		if (location != null && location.equals(Constants.ATTR_INTERPRETER_DEFAULT)){
			location = interpreterManager.getDefaultInterpreter();
            
		}else if(interpreterManager.hasInfoOnInterpreter(location) == false){
		    File file = new File(location);
		    if(!file.exists()){
		        throw new InvalidRunException("Error. The interprer: "+location+" does not exist");
		        
		    }else{
		        throw new InvalidRunException("Error. The interprer: "+location+" is not configured in the pydev preferences as a valid '"+nature.getVersion()+"' interpreter.");
		    }
        }
		return location;
	}
    
    
    /**
     * Expands and returns the python interprter attribute of the given launch
     * configuration. The intepreter path is verified to point to an existing
     * file in the local file system.
     * 
     * @param configuration launch configuration
     * @return an absolute path to the interpreter in the local file system
     * @throws CoreException if unable to retrieve the associated launch
     * configuration attribute, if unable to resolve any variables, or if the
     * resolved location does not point to an existing directory in the local
     * file system
     * @throws InvalidRunException 
     */
    public static IPath getInterpreter(ILaunchConfiguration configuration, IPythonNature nature) throws CoreException, InvalidRunException {
        String location = getInterpreterLocation(configuration, nature);
        if (location == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get python interpreter for run", null));
        } else {
            String expandedLocation = getStringVariableManager().performStringSubstitution(location);
            if (expandedLocation == null || expandedLocation.length() == 0) {
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get expanded interpreter for run", null));
            } else {
                return new Path(expandedLocation);
            }
        }
    }

    /**
     * Gets the project that should be used for a launch configuration
     * @param conf the launch configuration from where the project should be gotten
     * @return the related IProject
     * @throws CoreException
     */
    public static IProject getProjectFromConfiguration(ILaunchConfiguration conf) throws CoreException{
        String projName = conf.getAttribute(Constants.ATTR_PROJECT, "");
        if (projName == null || projName.length() == 0){
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get project for the run",null));
        }
        
        IWorkspace w = ResourcesPlugin.getWorkspace();
        IProject p = w.getRoot().getProject(projName);   
        if(p == null){ // Ok, we could not find it out
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not get project: " + projName, null));
        }        
        return p;
    }
    
    /**
     * Can be used to extract the pythonpath used from a given configuration.
     * 
     * @param conf the configuration from where we want to get the pythonpath
     * @return a string with the pythonpath used (with | as a separator)
     * @throws CoreException
     * @throws InvalidRunException 
     */
    public static String getPythonpathFromConfiguration(ILaunchConfiguration conf) throws CoreException, InvalidRunException{
        IProject p = getProjectFromConfiguration(conf);
        PythonNature pythonNature = PythonNature.getPythonNature(p);
        if (pythonNature == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Project should have a python nature: " + p.getName(), null));
        }
        String l = getInterpreterLocation(conf, pythonNature);
        return SimpleRunner.makePythonPathEnvString(pythonNature, l);
    }
    
    public PythonRunnerConfig(ILaunchConfiguration conf, String mode, String run) throws CoreException, InvalidRunException {
        this(conf, mode, run, true);
    }
    
	/**
	 * Sets defaults.
	 * @throws InvalidRunException 
	 */
	@SuppressWarnings("unchecked")
    public PythonRunnerConfig(ILaunchConfiguration conf, String mode, String run, boolean makeArgumentsVariableSubstitution) throws CoreException, InvalidRunException {
	    //1st thing, see if this is a valid run.
        project = getProjectFromConfiguration(conf);
        
        if(project == null){ //Ok, we could not find it out
            CoreException e = PydevPlugin.log("Could not get project for resource: "+resource);
            throw e;
        }

        // We need the project to find out the default interpreter from the InterpreterManager.
        IPythonNature pythonNature = PythonNature.getPythonNature(project);
        if (pythonNature == null) {
            CoreException e = PydevPlugin.log("No python nature for project: " + project.getName());
            throw e;
        }
        
        
        
        if(pythonNature.isJython()){
            if(!run.equals(RUN_JYTHON) && !run.equals(RUN_JYTHON_UNITTEST)){
                throw new InvalidRunException(StringUtils.format(
                        "Cannot make a '%s' for the project '%s' because the project is configured as %s\n\n" +
                        "To fix this, configure '%s' as python or run it as jython", 
                        run, project.getName(), pythonNature.getVersion(), project.getName()));
            }
        }else if(pythonNature.isPython()){
            if(!run.equals(RUN_REGULAR) && !run.equals(RUN_COVERAGE) && !run.equals(RUN_UNITTEST)){
                throw new InvalidRunException(StringUtils.format(
                        "Cannot make a '%s' for the project '%s' because the project is configured as %s\n\n" +
                        "To fix this, configure '%s' as jython or run it as python", 
                        run, project.getName(), pythonNature.getVersion(),  project.getName()));
            }
        }

	    
	    
	    //now, go on configuring other things
	    this.configuration = conf;
        this.run = run;
		isDebug = mode.equals(ILaunchManager.DEBUG_MODE);
		isInteractive = mode.equals("interactive");
		
        resource = getLocation(conf);
		arguments = getArguments(conf, makeArgumentsVariableSubstitution);
		IPath workingPath = getWorkingDirectory(conf);
		workingDirectory = workingPath == null ? null : workingPath.toFile();
		acceptTimeout = PydevPrefs.getPreferences().getInt(PydevPrefs.CONNECT_TIMEOUT);
        
        interpreterLocation = getInterpreterLocation(conf, pythonNature);
		interpreter = getInterpreter(conf, pythonNature);
        
        //make the environment
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        envp = launchManager.getEnvironment(conf);
        
        if(envp == null){
            //ok, the user has done nothing to the environment, just get all the default environment and
            //put the pythonpath in it
            envp = new SimplePythonRunner().getEnvironment(pythonNature, interpreterLocation);
            pythonpathUsed = SimpleRunner.makePythonPathEnvString(pythonNature, interpreterLocation);
        }else{
    		boolean win32= Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32);

    		//ok, the user has done something to configure it, so, just add the pythonpath to the
            //current env (if he still didn't do so)
    		Map envMap = conf.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map)null);

    		if(!specifiedPythonpath(envMap)){
	    		
	            String pythonpath = SimpleRunner.makePythonPathEnvString(pythonNature, interpreterLocation);
                pythonpathUsed = pythonpath; 
	            //override it if it was the ambient pythonpath
	            for (int i = 0; i < envp.length; i++) {
	                if(win32){
                        //case insensitive
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
	            envp = s;
		            
    		}
        }
	}
	
    /**
     * @param envMap
     * @return
     */
    private boolean specifiedPythonpath(Map<String, String> envMap) {
        if(envMap == null){
            return false;
            
        }else{
    		boolean win32= Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32);

            for (Iterator<String> iter = envMap.keySet().iterator(); iter.hasNext();) {
                String s = iter.next();

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
			debugPort= SocketUtil.findUnusedLocalPort();
			if (debugPort == -1)
				throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not find a free socket for debugger", null));
		}
		return debugPort;		
	}


    public static String getRunningName(IPath[] paths) {
    	FastStringBuffer buf = new FastStringBuffer(20*paths.length);
    	for(IPath p:paths){
    		if(buf.length() > 0){
    			buf.append(" - ");
    		}
    		buf.append(p.lastSegment());
    	}
    	return buf.toString();
    	
    }
    
    
	public String getRunningName() {
		return getRunningName(resource);
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
	 * @throws JDTNotAvailableException 
	 */
	public String[] getCommandLine(boolean makeVariableSubstitution) throws CoreException, JDTNotAvailableException {
		List<String> cmdArgs = new ArrayList<String>();
        
        if(isJython()){
            //"java.exe" -classpath "C:\bin\jython21\jython.jar" org.python.util.jython script %ARGS%
            String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getAbsolutePath();
            if(!InterpreterInfo.isJythonExecutable(interpreter.toOSString())){
                throw new RuntimeException("The jython jar must be specified as the interpreter to run. Found: "+interpreter);
            }
            cmdArgs.add(javaLoc);

            //some nice things on the classpath config: http://mindprod.com/jgloss/classpath.html
            cmdArgs.add("-classpath");
            String cpath;
            
            //TODO: add some option in the project so that the user can choose to use the
            //classpath specified in the java project instead of the pythonpath itself
            
//            if (project.getNature(Constants.JAVA_NATURE) != null){
//            	cpath  = getClasspath(JavaCore.create(project));
//            } else {
            	cpath = interpreter + SimpleRunner.getPythonPathSeparator() + pythonpathUsed;
//            }
            cmdArgs.add(cpath);
            cmdArgs.add("-Dpython.path="+pythonpathUsed); //will be added to the env variables in the run (check if this works on all platforms...)
            
           	addVmArgs(cmdArgs);
            	
            if (isDebug) {
            	cmdArgs.add("-Dpython.security.respectJavaAccessibility=false"); //TODO: the user should configure this -- we use it so that 
            																	 //we can access the variables during debugging. 
            	cmdArgs.add("org.python.util.jython");

            	cmdArgs.add(getDebugScript());
                cmdArgs.add("--vm_type");
                cmdArgs.add("jython");
                cmdArgs.add("--client");
                cmdArgs.add("localhost");
                cmdArgs.add("--port");
                cmdArgs.add(Integer.toString(debugPort));
                cmdArgs.add("--file");

            }else{
            	cmdArgs.add("org.python.util.jython");
            	
            }
            
            
            if(isUnittest()){
                cmdArgs.add(getRunFilesScript());
                cmdArgs.add("--verbosity");
                cmdArgs.add( PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_VERBOSITY) );
                
                String filter = PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_TEST_FILTER);
                if (filter.length() > 0) {
                    cmdArgs.add("--filter");
                    cmdArgs.add( filter );
                }
            }

        }else{
        
    		cmdArgs.add(interpreter.toOSString());
    		// Next option is for unbuffered stdout, otherwise Eclipse will not see any output until done
            cmdArgs.add("-u");
        
            addVmArgs(cmdArgs);
            
    		if (isDebug) {
    			cmdArgs.add(getDebugScript());
                cmdArgs.add("--vm_type");
                cmdArgs.add("python");
    			cmdArgs.add("--client");
    			cmdArgs.add("localhost");
    			cmdArgs.add("--port");
    			cmdArgs.add(Integer.toString(debugPort));
    			cmdArgs.add("--file");
    		}
    		
    		if(isCoverage()){
    			cmdArgs.add(getCoverageScript());
    			String coverageFileLocation = PyCoverage.getCoverageFileLocation();
                cmdArgs.add(coverageFileLocation);
    			cmdArgs.add("-x");
    			if (!isFile()){
    			    //run all testcases
                    cmdArgs.add(getRunFilesScript());
                }
    		}
    
    		if(isUnittest()){
                cmdArgs.add(getRunFilesScript());
                cmdArgs.add("--verbosity");
                cmdArgs.add( PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_VERBOSITY) );
                
                String filter = PydevPrefs.getPreferences().getString(PyunitPrefsPage.PYUNIT_TEST_FILTER);
                if (filter.length() > 0) {
	                cmdArgs.add("--filter");
	                cmdArgs.add( filter );
                }
    		}
        }
        
        for(IPath p:resource){
        	cmdArgs.add(p.toOSString());
        }
        
        String runArguments[] = null;
        if (makeVariableSubstitution && arguments != null) {
            String expanded = getStringVariableManager().performStringSubstitution(arguments);
            runArguments = parseStringIntoList(expanded);
        }

        for (int i=0; runArguments != null && i<runArguments.length; i++){
            cmdArgs.add(runArguments[i]);
        }
        
		String[] retVal = new String[cmdArgs.size()];
		cmdArgs.toArray(retVal);
		return retVal;
	}

    /**
     * @param cmdArgs
     * @throws CoreException
     */
    private void addVmArgs(List<String> cmdArgs) throws CoreException {
        String[] vmArguments = getVMArguments(configuration);
        if(vmArguments != null){
            for (int i = 0; i < vmArguments.length; i++){
            	cmdArgs.add(vmArguments[i]);
            }
        }
    }

	
	/**
	 * @return an array with the vm arguments in the given configuration.
	 * @throws CoreException
	 */
    private String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    	String args = configuration.getAttribute(Constants.ATTR_VM_ARGUMENTS, (String) null);
        if (args != null && args.trim().length() > 0) {
            String expanded = getStringVariableManager().performStringSubstitution(args);
            return parseStringIntoList(expanded);
       }
       return null;
	}

    /**
     * @return A command line to be shown to the user. Note that this command line should not actually be used for
     * an execution (only String[] should be passed to Runtie.exec)
     * @throws JDTNotAvailableException
     */
	public String getCommandLineAsString() throws JDTNotAvailableException {
		String[] args;
        try {
            args = getCommandLine(false);
            return SimpleRunner.getArgumentsAsStr(args);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
	}

}
