/*
 * Author: atotic
 * Created on Mar 18, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.osgi.framework.Bundle;
import org.python.pydev.debug.codecoverage.PyCoverage;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.debug.unittest.ITestRunListener;

/**
 * Holds configuration for PythonRunner.
 * 
 * It knows how to extract proper launching arguments from disparate sources. 
 * Has many launch utility functions (getCommandLine & friends).
 */
public class PythonRunnerConfig {

	public IPath file;
	public String interpreter;
	public String[] arguments;
	public File workingDirectory;
	// debugging
	public boolean isDebug;
	public boolean isProfile;
	private int debugPort = 0;  // use getDebugPort
	public int acceptTimeout = 5000; // miliseconds
	public String[] envp = null;

	public String debugScript;
	public String profileScript;

	// unit test specific
	public boolean isUnitTest;
	public String unitTestScript;
	private int unitTestPort = 0;  // use getUnitTestPort
	private String unitTestModule;
	private String unitTestModuleDir;
	
	/**
	 * Sets defaults.
	 */

	public PythonRunnerConfig(ILaunchConfiguration conf, String mode) throws CoreException {
		isDebug = mode.equals(ILaunchManager.DEBUG_MODE);
		isProfile = mode.equals(ILaunchManager.PROFILE_MODE);
		isUnitTest = mode.equals("unittest");
		
		file = ExternalToolsUtil.getLocation(conf);
		interpreter = conf.getAttribute(Constants.ATTR_INTERPRETER, "python");
		arguments = ExternalToolsUtil.getArguments(conf);
		IPath workingPath = ExternalToolsUtil.getWorkingDirectory(conf);
		workingDirectory = workingPath == null ? null : workingPath.toFile();
		acceptTimeout = PydevPrefs.getPreferences().getInt(PydevPrefs.CONNECT_TIMEOUT);

		if (isDebug) {
			debugScript = getDebugScript();
		}else if (isProfile){
		    profileScript = getProfileScript();
		}else if (isUnitTest){
		    unitTestScript = getUnitTestScript();
			setUnitTestInfo();
		}

		envp = DebugPlugin.getDefault().getLaunchManager().getEnvironment(conf);
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

    private void setUnitTestInfo() {
		try {
			setUnitTestPort();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	// get the test module name and path so that we can import it in Python
		int segmentCount = file.segmentCount();

		IPath noextPath = file.removeFileExtension();
		unitTestModule =  noextPath.lastSegment();
		IPath modulePath = file.uptoSegment(segmentCount-1);
		unitTestModuleDir = modulePath.toString();
    }
    
	public String getRunningName() {
		return file.lastSegment();
	}

	/**
	 * @throws CoreException if arguments are inconsistent
	 */
	public void verify() throws CoreException {
		if (file == null
			|| interpreter == null)
		throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Invalid PythonRunnerConfig",null));
		if (isDebug &&
			( acceptTimeout < 0
			|| debugPort < 0
			|| debugScript == null))
		throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Invalid PythonRunnerConfig",null));
	}

	/**
     * @return
	 * @throws CoreException
     */
    public static String getProfileScript() throws CoreException {
        return PydevDebugPlugin.getScriptWithinPySrc("coverage.py").getAbsolutePath();
    }

    public static String getUnitTestScript() throws CoreException {
        return PydevDebugPlugin.getScriptWithinPySrc("SocketTestRunner.py").getAbsolutePath();
        /*
    	String target = "SocketTestRunner.py";
        IPath relative = new Path("pysrc").addTrailingSeparator().append(target);

        Bundle bundle = PydevDebugPlugin.getDefault().getBundle();

        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            File f = new File(fileURL.getPath());

            return f.getAbsolutePath();
        } catch (IOException e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR,
                    "Can't find python script", null));
        }
    	*/
    }

    /** 
	 * gets location of jpydaemon.py
	 */
	public static String getDebugScript() throws CoreException {
	    return PydevDebugPlugin.getScriptWithinPySrc("pydevd.py").getAbsolutePath();

//	    IPath relative = new Path("pysrc").addTrailingSeparator().append("pydevd.py");
//		IPath relative = new Path("pysrc").addTrailingSeparator().append("jpydaemon.py");
//		IPath relative = new Path("pysrc").addTrailingSeparator().append("rpdb.py");
//		Bundle bundle = PydevDebugPlugin.getDefault().getBundle();
//		URL bundleURL = Platform.find( bundle, relative);
//		URL fileURL;
//		try {
//			fileURL = Platform.asLocalURL( bundleURL);	 
//			String filePath = new File(fileURL.getPath()).getAbsolutePath();
//			return filePath;
//		} catch (IOException e) {
//			throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Can't find python debug script", null));
//		}
	}

	/**
	 * Create a command line for launching.
	 * @return command line ready to be exec'd
	 */
	public String[] getCommandLine() {
		Vector cmdArgs = new Vector(10);
		cmdArgs.add(interpreter);
		// Next option is for unbuffered stdout, otherwise Eclipse will not see any output until done
		cmdArgs.add(org.python.pydev.ui.InterpreterEditor.isJython(interpreter) ? "-i" : "-u");
		if (isDebug) {
//	rpdb		cmdArgs.add(debugScript);
//			cmdArgs.add("-c");
//			cmdArgs.add("-p"+Integer.toString(debugPort));		
// jpydebug	cmdArgs.add(debugScript);
//			cmdArgs.add("localhost");
//			cmdArgs.add(Integer.toString(debugPort));
			cmdArgs.add(debugScript);
			cmdArgs.add("--client");
			cmdArgs.add("localhost");
			cmdArgs.add("--port");
			cmdArgs.add(Integer.toString(debugPort));
			cmdArgs.add("--file");
		}
		
		if(isProfile){
			cmdArgs.add(profileScript);
			cmdArgs.add(PyCoverage.getCoverageFileLocation());
			cmdArgs.add("-x");
		}

		if(isUnitTest){
			cmdArgs.add(unitTestScript);
			cmdArgs.add(Integer.toString(getUnitTestPort()));
			cmdArgs.add(unitTestModuleDir);
			cmdArgs.add(unitTestModule);
			String[] retVal = new String[cmdArgs.size()];
			cmdArgs.toArray(retVal);
			return retVal;
		}

		cmdArgs.add(file.toOSString());
		for (int i=0; arguments != null && i<arguments.length; i++)
			cmdArgs.add(arguments[i]);
		String[] retVal = new String[cmdArgs.size()];
		cmdArgs.toArray(retVal);
		return retVal;
	}
	
	
	public String getCommandLineAsString() {
		String[] args = getCommandLine();
		return getCommandLineAsString(args);
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
