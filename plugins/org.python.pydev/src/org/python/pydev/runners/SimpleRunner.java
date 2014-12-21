/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class SimpleRunner {

    /**
     * Passes the commands directly to Runtime.exec (with the passed envp)
     */
    public static Process createProcess(String[] cmdarray, String[] envp, File workingDir) throws IOException {
        return ProcessUtils.createProcess(cmdarray, envp, workingDir);
    }

    /**
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     *
     * changed so that we always set the PYTHONPATH in the environment
     *
     * @return the system environment with the PYTHONPATH env variable added for a given project (if it is null, return it with the
     * default PYTHONPATH added).
     */
    public static String[] getEnvironment(IPythonNature pythonNature, IInterpreterInfo interpreter,
            IInterpreterManager manager) throws CoreException {
        String[] env;

        String pythonPathEnvStr = "";
        try {

            if (interpreter != null) { //check if we have a default interpreter.
                pythonPathEnvStr = makePythonPathEnvString(pythonNature, interpreter, manager);
            }
            env = createEnvWithPythonpath(pythonPathEnvStr, pythonNature, manager);

        } catch (Exception e) {
            Log.log(e);
            //We cannot get it. Log it and keep with the default.
            env = getDefaultSystemEnvAsArray(pythonNature);
        }

        if (interpreter != null) {
            env = interpreter.updateEnv(env);
        }

        return env;
    }

    /**
     * Same as the getEnvironment, but with a pre-specified pythonpath.
     * @throws MisconfigurationException
     */
    public static String[] createEnvWithPythonpath(String pythonPathEnvStr, String interpreter,
            IInterpreterManager manager, IPythonNature nature) throws CoreException, MisconfigurationException {
        String[] env = createEnvWithPythonpath(pythonPathEnvStr, nature, manager);
        IInterpreterInfo info = manager.getInterpreterInfo(interpreter, new NullProgressMonitor());
        env = info.updateEnv(env);
        return env;
    }

    private static String[] createEnvWithPythonpath(String pythonPathEnvStr, IPythonNature nature,
            IInterpreterManager manager) throws CoreException {
        if (SharedCorePlugin.inTestMode()) {
            return null;
        }

        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        Map<String, String> env = getDefaultSystemEnv(defaultPlugin, nature); //no need to remove as it'll be updated

        env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
        switch (manager.getInterpreterType()) {

            case IPythonNature.INTERPRETER_TYPE_JYTHON:
                env.put("CLASSPATH", pythonPathEnvStr); //put the environment
                env.put("JYTHONPATH", pythonPathEnvStr); //put the environment
                break;

            case IPythonNature.INTERPRETER_TYPE_IRONPYTHON:
                env.put("IRONPYTHONPATH", pythonPathEnvStr); //put the environment

                break;
        }
        return getMapEnvAsArray(env);
    }

    /**
     * @return an array with the env variables for the system with the format xx=yy
     */
    public static String[] getDefaultSystemEnvAsArray(IPythonNature nature) throws CoreException {
        Map<String, String> defaultSystemEnv = getDefaultSystemEnv(nature);
        if (defaultSystemEnv != null) {
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }

    /**
     * @return a map with the env variables for the system
     */
    public static Map<String, String> getDefaultSystemEnv(IPythonNature nature) throws CoreException {
        if (SharedCorePlugin.inTestMode()) {
            return null;
        }

        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        return getDefaultSystemEnv(defaultPlugin, nature);
    }

    /**
     * @return a map with the env variables for the system
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> getDefaultSystemEnv(DebugPlugin defaultPlugin, IPythonNature nature)
            throws CoreException {
        ILaunchManager launchManager = defaultPlugin.getLaunchManager();

        // build base environment
        Map<String, String> env = new HashMap<String, String>();
        env.putAll(launchManager.getNativeEnvironment());

        // Add variables from config
        boolean win32 = PlatformUtils.isWindowsPlatform();
        for (Iterator iter = env.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            if (win32) {
                // Win32 vars are case insensitive. Uppercase everything so
                // that (for example) "pAtH" will correctly replace "PATH"
                key = key.toUpperCase();
            }
            String value = (String) entry.getValue();
            // translate any string substitution variables
            String translated = value;
            try {
                StringSubstitution stringSubstitution = new StringSubstitution(nature);
                translated = stringSubstitution.performStringSubstitution(value, false);
            } catch (Exception e) {
                Log.log(e);
            }
            env.put(key, translated);
        }

        //Always remove PYTHONHOME from the default system env, as it doesn't work well with multiple interpreters.
        env.remove("PYTHONHOME");
        // PyDev-495 Remove VIRTUAL_ENV as it cause IPython to munge the PYTHON_PATH
        env.remove("VIRTUAL_ENV");
        return env;
    }

    /**
     * copied from org.eclipse.jdt.internal.launching.StandardVMRunner
     * @param args - other arguments to be added to the command line (may be null)
     * @return
     */
    public static String getArgumentsAsStr(String[] commandLine, String... args) {
        return ProcessUtils.getArgumentsAsStr(commandLine, args);
    }

    /**
     * Creates a string that can be passed as the PYTHONPATH
     *
     * @param project the project we want to get the settings from. If it is null, the system pythonpath is returned
     * @param interpreter this is the interpreter to be used to create the env.
     * @return a string that can be used as the PYTHONPATH env variable
     */
    public static String makePythonPathEnvString(IPythonNature pythonNature, IInterpreterInfo interpreter,
            IInterpreterManager manager) {
        if (pythonNature == null) {
            if (interpreter == null) {
                return makePythonPathEnvFromPaths(new ArrayList<String>()); //no pythonpath can be gotten (set to empty, so that the default is gotten)
            } else {
                List<String> pythonPath = interpreter.getPythonPath();
                return makePythonPathEnvFromPaths(pythonPath);
            }
        }

        List<String> paths;

        //if we have a project, get its complete pythonpath
        IPythonPathNature pythonPathNature = pythonNature.getPythonPathNature();
        if (pythonPathNature == null) {
            IProject project = pythonNature.getProject();
            String projectName;
            if (project == null) {
                projectName = "null?";
            } else {
                projectName = project.getName();
            }
            throw new RuntimeException("The project " + projectName + " does not have the pythonpath configured, \n"
                    + "please configure it correcly (please check the pydev getting started guide at \n"
                    + "http://pydev.org/manual_101_root.html for better information on how to do it).");
        }
        paths = pythonPathNature.getCompleteProjectPythonPath(interpreter, manager);

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

            paths.add(0, FileUtils.getFileAbsolutePath(PydevPlugin.getScriptWithinPySrc("pydev_sitecustomize")));
        } catch (CoreException e) {
            Log.log(e);
        }

        String separator = getPythonPathSeparator();
        return StringUtils.join(separator, paths);
    }

    /**
     * @return the separator for the pythonpath variables (system dependent)
     */
    public static String getPythonPathSeparator() {
        return System.getProperty("path.separator"); //is system dependent, and should cover for all cases...
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
    public static String[] getMapEnvAsArray(Map<String, String> env) {
        return ProcessUtils.getMapEnvAsArray(env);
    }

    public Tuple<Process, String> run(String[] cmdarray, File workingDir, IPythonNature nature, IProgressMonitor monitor) {
        return run(cmdarray, workingDir, nature, monitor, null);
    }

    /**
     * @return a tuple with the process created and a string representation of the cmdarray.
     */
    public Tuple<Process, String> run(String[] cmdarray, File workingDir, IPythonNature nature,
            IProgressMonitor monitor, ICallback<String[], String[]> updateEnv) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        String executionString = getArgumentsAsStr(cmdarray);
        monitor.setTaskName("Executing: " + executionString);
        monitor.worked(5);
        Process process = null;
        try {
            monitor.setTaskName("Making pythonpath environment..." + executionString);
            String[] envp = null;
            if (nature != null) {
                envp = getEnvironment(nature, nature.getProjectInterpreter(), nature.getRelatedInterpreterManager()); //Don't remove as it *should* be updated based on the nature)
            }
            //Otherwise, use default (used when configuring the interpreter for instance).
            monitor.setTaskName("Making exec..." + executionString);
            if (workingDir != null) {
                if (!workingDir.isDirectory()) {
                    throw new RuntimeException(StringUtils.format(
                            "Working dir must be an existing directory (received: %s)", workingDir));
                }
            }
            if (updateEnv != null) {
                envp = updateEnv.call(envp);
            }
            process = createProcess(cmdarray, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Tuple<Process, String>(process, executionString);
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
    public Tuple<String, String> runAndGetOutput(String[] cmdarray, File workingDir, IPythonNature nature,
            IProgressMonitor monitor, String encoding) {
        Tuple<Process, String> r = run(cmdarray, workingDir, nature, monitor);

        return getProcessOutput(r.o1, r.o2, monitor, encoding);
    }

    /**
     * @param process process from where the output should be gotten
     * @param executionString string to execute (only for errors)
     * @param monitor monitor for giving progress
     * @return a tuple with the output of stdout and stderr
     */
    public static Tuple<String, String> getProcessOutput(Process process, String executionString,
            IProgressMonitor monitor, String encoding) {
        return ProcessUtils.getProcessOutput(process, executionString, monitor, encoding);
    }

    /**
     * @param pythonpath the pythonpath string to be used
     * @return a list of strings with the elements of the pythonpath
     */
    public static List<String> splitPythonpath(String pythonpath) {
        ArrayList<String> splitted = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(pythonpath, getPythonPathSeparator());
        while (tokenizer.hasMoreTokens()) {
            splitted.add(tokenizer.nextToken());
        }
        return splitted;

    }

}
