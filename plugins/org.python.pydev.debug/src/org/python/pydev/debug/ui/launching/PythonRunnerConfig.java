/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 18, 2004
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.ast.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.listing_utils.JavaVmLocationFinder;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.debug.codecoverage.PyCodeCoverageView;
import org.python.pydev.debug.codecoverage.PyCoverage;
import org.python.pydev.debug.codecoverage.PyCoveragePreferences;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.ListenConnector;
import org.python.pydev.debug.profile.PyProfilePreferences;
import org.python.pydev.debug.pyunit.PyUnitServer;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.debug.ui.RunPreferencesPage;
import org.python.pydev.debug.ui.launching.PythonRunnerCallbacks.CreatedCommandLineParams;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.net.LocalHost;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

/**
 * Holds configuration for PythonRunner.
 *
 * It knows how to extract proper launching arguments from disparate sources.
 * Has many launch utility functions (getCommandLine & friends).
 */
public class PythonRunnerConfig {

    public static final String RUN_COVERAGE = "python code coverage run";
    public static final String RUN_REGULAR = "python regular run";
    public static final String RUN_UNITTEST = "pyton unittest run";
    public static final String RUN_JYTHON_UNITTEST = "jython unittest run";
    public static final String RUN_JYTHON = "jython regular run";
    public static final String RUN_IRONPYTHON = "iron python regular run";
    public static final String RUN_IRONPYTHON_UNITTEST = "iron python unittest run";

    public final IProject project;
    public final IPath[] resource;
    public final IPath interpreter;
    public final IInterpreterInfo interpreterLocation;
    private final String arguments;
    public final File workingDirectory;
    public final String pythonpathUsed;

    // debugging
    public final boolean isDebug;
    public final boolean isInteractive;
    public int acceptTimeout = 5000; // miliseconds
    public String[] envp = null;

    /** One of RUN_ enums */
    public final String run;
    private final ILaunchConfiguration configuration;
    private ListenConnector listenConnector;
    private PyUnitServer pyUnitServer;

    //    public boolean isCoverage(){
    //        return this.run.equals(RUN_COVERAGE);
    //    }

    public boolean isUnittest() {
        return this.run.equals(RUN_UNITTEST) || this.run.equals(RUN_JYTHON_UNITTEST)
                || this.run.equals(RUN_IRONPYTHON_UNITTEST);
    }

    public boolean isJython() {
        return this.run.equals(RUN_JYTHON) || this.run.equals(RUN_JYTHON_UNITTEST);
    }

    public boolean isIronpython() {
        return this.run.equals(RUN_IRONPYTHON) || this.run.equals(RUN_IRONPYTHON_UNITTEST);
    }

    public boolean isFile() throws CoreException {
        int resourceType = configuration.getAttribute(Constants.ATTR_RESOURCE_TYPE, -1);
        return resourceType == IResource.FILE;
    }

    /*
     * Expands and returns the location attribute of the given launch configuration. The location is verified to point
     * to an existing file, in the local file system.
     *
     * @param configuration launch configuration
     *
     * @return an absolute path to a file in the local file system
     *
     * @throws CoreException if unable to retrieve the associated launch configuration attribute, if unable to resolve
     * any variables, or if the resolved location does not point to an existing file in the local file system
     */
    public static IPath[] getLocation(ILaunchConfiguration configuration, IPythonNature nature) throws CoreException {
        String locationsStr = configuration.getAttribute(Constants.ATTR_ALTERNATE_LOCATION, (String) null);
        if (locationsStr == null) {
            locationsStr = configuration.getAttribute(Constants.ATTR_LOCATION, (String) null);
        }
        if (locationsStr == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get location for run", null));
        }

        List<String> locations = StringUtils.splitAndRemoveEmptyTrimmed(locationsStr, '|');
        Path[] ret = new Path[locations.size()];
        int i = 0;
        for (String location : locations) {
            String expandedLocation = getStringSubstitution(nature).performStringSubstitution(location);
            if (expandedLocation == null || expandedLocation.length() == 0) {
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                        "Unable to get expanded location for run", null));
            } else {
                ret[i] = new Path(expandedLocation);
            }
            i++;
        }
        return ret;
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
    public static String getArguments(ILaunchConfiguration configuration, boolean makeArgumentsVariableSubstitution)
            throws CoreException {
        String arguments = configuration.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "");
        if (makeArgumentsVariableSubstitution) {
            return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(arguments);
        } else {
            return arguments;
        }
    }

    private static StringSubstitution getStringSubstitution(IPythonNature nature) {
        return new StringSubstitution(nature);
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
    public static IPath getWorkingDirectory(ILaunchConfiguration configuration, IPythonNature nature)
            throws CoreException {
        IProject project = nature.getProject();
        String location = configuration.getAttribute(Constants.ATTR_WORKING_DIRECTORY,
                "${project_loc:/" + project.getName() + "}");
        if (location != null) {
            String expandedLocation = getStringSubstitution(nature).performStringSubstitution(location);
            if (expandedLocation.length() > 0) {
                File path = new File(expandedLocation);
                if (path.isDirectory()) {
                    return new Path(expandedLocation);
                }
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                        "Unable to get working location for the run \n(the location: '" + expandedLocation
                                + "' is not a valid directory).",
                        null));
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
     * @throws MisconfigurationException
     */
    public static IInterpreterInfo getInterpreterLocation(ILaunchConfiguration conf, IPythonNature nature,
            IInterpreterManager interpreterManager) throws InvalidRunException, CoreException,
            MisconfigurationException {
        String location = conf.getAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);

        if (location != null && location.equals(Constants.ATTR_INTERPRETER_DEFAULT)) {
            if (nature != null && nature.getInterpreterType() == interpreterManager.getInterpreterType()) {

                //When both, the interpreter for the launch and the nature have the same type, let's get the
                //launch location from the project
                try {
                    return nature.getProjectInterpreter();
                } catch (PythonNatureWithoutProjectException e) {
                    throw new RuntimeException(e);
                }

            } else {

                //When it doesn't have the same type it means that we're trying to run as jython a python
                //project (or vice-versa), so, we must get the interpreter from the interpreter manager!
                return interpreterManager.getDefaultInterpreterInfo(true);
            }

        } else {
            IInterpreterInfo interpreterInfo = interpreterManager.getInterpreterInfo(location, null);
            if (interpreterInfo != null) {
                return interpreterInfo;
            } else {
                File file = new File(location);
                if (!file.exists()) {
                    throw new InvalidRunException("Error. The interprer: " + location + " does not exist");

                } else {
                    //it does not have information on the given interpreter!!
                    if (nature == null) {
                        throw new InvalidRunException("Error. The interpreter: >>" + location
                                + "<< is not configured in the pydev preferences as a valid interpreter (null nature).");
                    } else {
                        throw new InvalidRunException("Error. The interpreter: >>" + location
                                + "<< is not configured in the pydev preferences as a valid '"
                                + nature.getVersion(false)
                                + "' interpreter.");
                    }
                }
            }
        }
    }

    /**
     * Expands and returns the python interpreter attribute of the given launch
     * configuration. The interpreter path is verified to point to an existing
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
    private IPath getInterpreter(IInterpreterInfo location, ILaunchConfiguration configuration, IPythonNature nature)
            throws CoreException, InvalidRunException {
        if (location == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                    "Unable to get python interpreter for run", null));
        } else {
            String expandedLocation = getStringSubstitution(nature).performStringSubstitution(
                    location.getExecutableOrJar());
            if (expandedLocation == null || expandedLocation.length() == 0) {
                throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                        "Unable to get expanded interpreter for run", null));
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
    public static IProject getProjectFromConfiguration(ILaunchConfiguration conf) throws CoreException {
        String projName = conf.getAttribute(Constants.ATTR_PROJECT, "");
        if (projName == null || projName.length() == 0) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Unable to get project for the run",
                    null));
        }

        IWorkspace w = ResourcesPlugin.getWorkspace();
        IProject p = w.getRoot().getProject(projName);
        if (p == null || !p.exists()) { // Ok, we could not find it out
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not get project: " + projName,
                    null));
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
     * @throws MisconfigurationException
     */
    public static String getPythonpathFromConfiguration(ILaunchConfiguration conf, IInterpreterManager manager)
            throws CoreException, InvalidRunException, MisconfigurationException {
        IProject p = getProjectFromConfiguration(conf);
        PythonNature pythonNature = PythonNature.getPythonNature(p);
        if (pythonNature == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Project should have a python nature: "
                    + p.getName(), null));
        }
        IInterpreterInfo l = getInterpreterLocation(conf, pythonNature, manager);
        return SimpleRunner.makePythonPathEnvString(pythonNature, l, manager);
    }

    public PythonRunnerConfig(ILaunchConfiguration conf, String mode, String run) throws CoreException,
            InvalidRunException, MisconfigurationException {
        this(conf, mode, run, true);
    }

    /**
     * Sets defaults.
     * @throws InvalidRunException
     * @throws MisconfigurationException
     */
    @SuppressWarnings("unchecked")
    public PythonRunnerConfig(ILaunchConfiguration conf, String mode, String run,
            boolean makeArgumentsVariableSubstitution) throws CoreException, InvalidRunException,
            MisconfigurationException {
        //1st thing, see if this is a valid run.
        project = getProjectFromConfiguration(conf);

        if (project == null) { //Ok, we could not find it out
            throw Log.log("Could not get project for configuration: " + conf);
        }

        // We need the project to find out the default interpreter from the InterpreterManager.
        IPythonNature pythonNature = PythonNature.getPythonNature(project);
        if (pythonNature == null) {
            CoreException e = Log.log("No python nature for project: " + project.getName());
            throw e;
        }

        //now, go on configuring other things
        this.configuration = conf;
        this.run = run;
        isDebug = mode.equals(ILaunchManager.DEBUG_MODE);
        isInteractive = mode.equals("interactive");

        resource = getLocation(conf, pythonNature);
        arguments = getArguments(conf, makeArgumentsVariableSubstitution);
        IPath workingPath = getWorkingDirectory(conf, pythonNature);
        workingDirectory = workingPath == null ? null : workingPath.toFile();
        acceptTimeout = PydevPrefs.getEclipsePreferences().getInt(PyDevEditorPreferences.CONNECT_TIMEOUT,
                PyDevEditorPreferences.DEFAULT_CONNECT_TIMEOUT);

        interpreterLocation = getInterpreterLocation(conf, pythonNature, this.getRelatedInterpreterManager());
        interpreter = getInterpreter(interpreterLocation, conf, pythonNature);

        //make the environment
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        envp = launchManager.getEnvironment(conf);
        IInterpreterManager manager;
        if (isJython()) {
            manager = InterpreterManagersAPI.getJythonInterpreterManager();
        } else if (isIronpython()) {
            manager = InterpreterManagersAPI.getIronpythonInterpreterManager();
        } else {
            manager = InterpreterManagersAPI.getPythonInterpreterManager();
        }

        boolean win32 = PlatformUtils.isWindowsPlatform();

        if (envp == null) {
            //ok, the user has done nothing to the environment, just get all the default environment which has the pythonpath in it
            envp = SimpleRunner.getEnvironment(pythonNature, interpreterLocation, manager);

        } else {
            //ok, the user has done something to configure it, so, just add the pythonpath to the
            //current env (if he still didn't do so)
            Map envMap = conf.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map) null);

            String pythonpath = SimpleRunner.makePythonPathEnvString(pythonNature, interpreterLocation, manager);
            updateVar(pythonNature, manager, win32, envMap, "PYTHONPATH", pythonpath);
            if (isJython()) {
                //Also update the classpath env variable.
                updateVar(pythonNature, manager, win32, envMap, "CLASSPATH", pythonpath);
                // And the jythonpath env variable
                updateVar(pythonNature, manager, win32, envMap, "JYTHONPATH", pythonpath);

            } else if (isIronpython()) {
                //Also update the ironpythonpath env variable.
                updateVar(pythonNature, manager, win32, envMap, "IRONPYTHONPATH", pythonpath);

            }

            //And we also must get the environment variables specified in the interpreter manager.
            envp = interpreterLocation.updateEnv(envp, envMap.keySet());
        }

        boolean hasDjangoNature = project.hasNature(PythonNature.DJANGO_NATURE_ID);

        String settingsModule = null;
        Map<String, String> variableSubstitution = null;
        final String djangoSettingsKey = "DJANGO_SETTINGS_MODULE";
        String djangoSettingsEnvEntry = null;
        try {
            variableSubstitution = pythonNature.getPythonPathNature().getVariableSubstitution();
            settingsModule = variableSubstitution.get(djangoSettingsKey);
            if (settingsModule != null) {
                if (settingsModule.trim().length() > 0) {
                    djangoSettingsEnvEntry = djangoSettingsKey + "=" + settingsModule.trim();
                }
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
        if (djangoSettingsEnvEntry == null && hasDjangoNature) {
            //Default if not specified (only add it if the nature is there).
            djangoSettingsEnvEntry = djangoSettingsKey + "=" + project.getName() + ".settings";
        }

        //Note: set flag even if not debugging as the user may use remote-debugging later on.
        boolean geventSupport = DebugPrefsPage.getGeventDebugging()
                && pythonNature.getInterpreterType() == IPythonNature.INTERPRETER_TYPE_PYTHON;

        //Now, set the pythonpathUsed according to what's in the environment.
        String p = "";
        for (int i = 0; i < envp.length; i++) {
            String s = envp[i];
            Tuple<String, String> tup = StringUtils.splitOnFirst(s, '=');
            String var = tup.o1;
            if (win32) {
                //On windows it doesn't matter, always consider uppercase.
                var = var.toUpperCase();
            }

            if (var.equals("PYTHONPATH")) {
                p = tup.o2;

            } else if (var.equals(djangoSettingsKey)) {
                //Update it.
                if (djangoSettingsEnvEntry != null) {
                    envp[i] = djangoSettingsEnvEntry;
                    djangoSettingsEnvEntry = null;
                }
            }

            if (geventSupport) {
                if (var.equals("GEVENT_SUPPORT")) {
                    //Flag already set in the environment
                    geventSupport = false;
                }
            }
        }

        //Still not added, let's do that now.
        if (djangoSettingsEnvEntry != null) {
            envp = StringUtils.addString(envp, djangoSettingsEnvEntry);
        }
        if (geventSupport) {
            envp = StringUtils.addString(envp, "GEVENT_SUPPORT=True");
        }

        // Get project and dependencies to calculate IDE_PROJECT_ROOTS.
        Set<IProject> projects = new HashSet<>();
        projects.add(project);
        projects.addAll(ProjectModulesManager.getReferencedProjects(project));
        Set<IResource> projectSourcePathFolderSet = new HashSet<>();
        for (IProject iProject : projects) {
            PythonNature n = PythonNature.getPythonNature(iProject);
            if (n != null) {
                projectSourcePathFolderSet.addAll(n.getPythonPathNature().getProjectSourcePathFolderSet());
            }
        }

        List<String> ideProjectRoots = new ArrayList<>();
        for (IResource iResource : projectSourcePathFolderSet) {
            String iResourceOSString = SharedCorePlugin.getIResourceOSString(iResource);
            if (iResourceOSString != null && !iResourceOSString.isEmpty()) {
                ideProjectRoots.add(iResourceOSString);
            }
        }
        envp = StringUtils.addString(envp,
                "IDE_PROJECT_ROOTS=" + StringUtils.join(File.pathSeparator, ideProjectRoots));
        envp = StringUtils.addString(envp,
                "PYDEVD_SHOW_COMPILE_CYTHON_COMMAND_LINE=True");
        this.pythonpathUsed = p;
    }

    @SuppressWarnings("unchecked")
    private void updateVar(IPythonNature pythonNature, IInterpreterManager manager, boolean win32, Map envMap,
            String var, String pythonpath) {

        if (!specifiedEnvVar(envMap, var)) {
            boolean addPythonpath = true;
            //override it if it was the ambient pythonpath
            for (int i = 0; i < envp.length; i++) {
                if (win32) {
                    //case insensitive
                    if (envp[i].toUpperCase().startsWith(var + "=")) {
                        //OK, finish it.
                        envp[i] = var + "=" + pythonpath;
                        addPythonpath = false;
                        break;
                    }
                } else {
                    if (envp[i].startsWith(var + "=")) {
                        //OK, finish it.
                        envp[i] = var + "=" + pythonpath;
                        addPythonpath = false;
                        break;
                    }
                }

            }

            if (addPythonpath) {
                //there was no pythonpath, let's set it
                String[] s = new String[envp.length + 1];
                System.arraycopy(envp, 0, s, 0, envp.length);
                s[s.length - 1] = var + "=" + pythonpath;
                envp = s;
            }
        }
    }

    /**
     * Check if map the passed env var key.
     *
     * Variables names are considered not case sensitive on Windows.
     *
     * @param envMap mapping of env variables and their values
     * @return {@code true} if passed map contain PYTHONPATH key.
     */
    private boolean specifiedEnvVar(Map<String, String> envMap, String var) {
        if (envMap == null) {
            return false;
        }
        boolean win32 = Platform.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32);

        if (!win32) {
            return envMap.containsKey(var);
        }

        //it is windows (consider all uppercase)
        var = var.toUpperCase();
        for (Iterator<String> iter = envMap.keySet().iterator(); iter.hasNext();) {
            String s = iter.next();
            if (s.toUpperCase().equals(var)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return attribute value of {@code IProcess.ATTR_PROCESS_TYPE}
     */
    public String getProcessType() {
        return isJython() ? "java" : Constants.PROCESS_TYPE;
    }

    public static String getRunningName(IPath[] paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }
        FastStringBuffer buf = new FastStringBuffer(20 * paths.length);
        for (IPath p : paths) {
            if (buf.length() > 0) {
                buf.append(" - ");
            }
            buf.append(p.lastSegment());
        }
        return buf.toString();

    }

    public String getConsoleLabel(String[] commandLine) {
        ArrayList<String> lst = new ArrayList<>();
        lst.add(getRunningName(resource));
        if (isDebug) {
            lst.add(" [debug]");
        }
        if (isUnittest()) {
            lst.add(" [unittest]");
        }
        if (isInteractive) {
            lst.add(" [interactive]");
        }
        if (PyProfilePreferences.getAllRunsDoProfile()) {
            lst.add(" [profile]");
        }
        if (PyCoveragePreferences.getAllRunsDoCoverage()) {
            lst.add(" [coverage]");
        }

        if (commandLine.length > 0) {
            lst.add(" [");
            lst.add(commandLine[0]);
            lst.add("]");
        }
        return StringUtils.join("", lst);
    }

    /**
     * @return
     * @throws CoreException
     */
    public static String getCoverageScript() throws CoreException {
        return FileUtils.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("pydev_coverage.py"));
    }

    /**
     * Gets location of pydevd.py
     * @note: Used on scripting (variables related to debugger location).
     */
    public static String getDebugScript() throws CoreException {
        return FileUtils.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("pydevd.py"));
    }

    public static String getRunFilesScript() throws CoreException {
        return FileUtils.getFileAbsolutePath(PydevDebugPlugin.getScriptWithinPySrc("runfiles.py"));
    }

    /**
     * Create a command line for launching.
     *
     * @param actualRun if true it'll make the variable substitution and start the listen connector in the case
     * of a debug session.
     *
     * @return command line ready to be exec'd
     * @throws CoreException
     * @throws JDTNotAvailableException
     */
    public String[] getCommandLine(boolean actualRun) throws CoreException, JDTNotAvailableException {
        List<String> cmdArgs = new ArrayList<String>();

        boolean profileRun = PyProfilePreferences.getAllRunsDoProfile();
        boolean coverageRun = PyCoveragePreferences.getAllRunsDoCoverage();

        boolean addWithDashMFlag = false;
        String modName = null;
        if (resource.length == 1 && RunPreferencesPage.getLaunchWithMFlag() && !isUnittest() && !coverageRun
                && !isInteractive) {
            IPath p = resource[0];
            String osString = p.toOSString();
            PythonNature pythonNature = PythonNature.getPythonNature(project);
            modName = pythonNature.resolveModule(osString);
            if (modName != null) {
                addWithDashMFlag = true;
            }
        }

        if (isJython()) {
            //"java.exe" -classpath "C:\bin\jython21\jython.jar" org.python.util.jython script %ARGS%
            String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getAbsolutePath();
            if (!InterpreterInfo.isJythonExecutable(interpreter.toOSString())) {
                throw new RuntimeException("The jython jar must be specified as the interpreter to run. Found: "
                        + interpreter);
            }
            cmdArgs.add(javaLoc);

            //some nice things on the classpath config: http://mindprod.com/jgloss/classpath.html
            cmdArgs.add("-classpath");
            String cpath;

            //TODO: add some option in the project so that the user can choose to use the
            //classpath specified in the java project instead of the pythonpath itself

            //            if (project.getNature(Constants.JAVA_NATURE) != null){
            //                cpath  = getClasspath(JavaCore.create(project));
            //            } else {
            cpath = interpreter + SimpleRunner.getPythonPathSeparator() + pythonpathUsed;
            //            }
            cmdArgs.add(cpath);
            cmdArgs.add("-Dpython.path=" + pythonpathUsed); //will be added to the env variables in the run (check if this works on all platforms...)

            addVmArgs(cmdArgs);
            addProfileArgs(cmdArgs, profileRun, actualRun);

            if (isDebug) {
                //This was removed because it cannot be used. See:
                //http://bugs.jython.org/issue1438
                //cmdArgs.add("-Dpython.security.respectJavaAccessibility=false");

                cmdArgs.add("org.python.util.jython");
                addDebugArgs(cmdArgs, "jython", actualRun, addWithDashMFlag, modName);
            } else {
                cmdArgs.add("org.python.util.jython");
            }

        } else {
            //python or iron python

            cmdArgs.add(interpreter.toOSString());
            // Next option is for unbuffered stdout, otherwise Eclipse will not see any output until done
            cmdArgs.add("-u");

            addVmArgs(cmdArgs);
            addProfileArgs(cmdArgs, profileRun, actualRun);

            if (isDebug && isIronpython()) {
                addIronPythonDebugVmArgs(cmdArgs);
            }

            addDebugArgs(cmdArgs, "python", actualRun, addWithDashMFlag, modName);
        }

        //Check if we should do code-coverage...
        if (coverageRun && isDebug) {
            if (actualRun) {
                RunInUiThread.async(new Runnable() {

                    @Override
                    public void run() {
                        PyDialogHelpers
                                .openWarning(
                                        "Conflicting options: coverage with debug.",
                                        "Making a debug run with coverage enabled will not yield the expected results.\n\n"
                                                + "They'll conflict because both use the python tracing facility (i.e.: sys.settrace()).\n"
                                                + "\n"
                                                + "To debug a coverage run, do a regular run and use the remote debugger "
                                                + "(but note that the coverage will stop when it's enabled).\n" + "\n"
                                                + "Note: the run will be continued anyways.");
                    }
                });
            }
        }

        if (isUnittest()) {
            cmdArgs.add(getRunFilesScript());
        } else {
            if (coverageRun) {
                //Separate support (unittest has the coverage support builtin).
                cmdArgs.add(getCoverageScript());
                cmdArgs.add(PyCoverage.getCoverageFileLocation().getAbsolutePath());
                cmdArgs.add("run");
                cmdArgs.add("--source");
                cmdArgs.add(PyCodeCoverageView.getChosenDir().getLocation().toOSString());
            }
        }

        if (!addWithDashMFlag) {
            for (IPath p : resource) {
                cmdArgs.add(p.toOSString());
            }
        } else {
            if (!isDebug) {
                cmdArgs.add("-m");
                cmdArgs.add(modName);
            }
        }

        if (!isUnittest()) {
            //The program arguments are not used when running a unittest (excluded from the tab group in favor
            //of a way to overriding the default unittest arguments).
            String runArguments[] = null;
            if (actualRun && arguments != null) {
                String expanded = getStringSubstitution(PythonNature.getPythonNature(project))
                        .performStringSubstitution(arguments);
                runArguments = ProcessUtils.parseArguments(expanded);
            }

            for (int i = 0; runArguments != null && i < runArguments.length; i++) {
                cmdArgs.add(runArguments[i]);
            }

        } else {
            //Last thing (first the files and last the special parameters the user passed -- i.e.: nose parameters)
            addUnittestArgs(cmdArgs, actualRun, coverageRun);
        }

        String[] retVal = new String[cmdArgs.size()];
        cmdArgs.toArray(retVal);

        if (actualRun) {
            CreatedCommandLineParams createdCommandLineParams = new CreatedCommandLineParams(retVal, coverageRun);
            //Provide a way for clients to alter the command line.
            List<Object> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMMAND_LINE_PARTICIPANT);
            for (Object object : participants) {
                try {
                    IPyCommandLineParticipant c = (IPyCommandLineParticipant) object;
                    createdCommandLineParams = c.updateCommandLine(createdCommandLineParams);
                } catch (Exception e) {
                    Log.log(e);
                }
            }

            retVal = createdCommandLineParams.cmdLine;
            PythonRunnerCallbacks.onCreatedCommandLine.call(createdCommandLineParams);
        }

        return retVal;
    }

    private void addProfileArgs(List<String> cmdArgs, boolean profileRun, boolean actualRun) {
        PyProfilePreferences.addProfileArgs(cmdArgs, profileRun, actualRun);
    }

    private void addIronPythonDebugVmArgs(List<String> cmdArgs) {
        if (cmdArgs.contains("-X:FullFrames")) {
            return;
        }
        //The iron python debugger must have frames (preferably FullFrames), otherwise it won't work.
        cmdArgs.add("-X:FullFrames");
    }

    /**
     * Adds a set of arguments used to wrap executed file with unittest runner.
     * @param actualRun in an actual run we'll start the xml-rpc server.
     * @param coverageRun whether we should add the flags to do a coverage run.
     */
    private void addUnittestArgs(List<String> cmdArgs, boolean actualRun, boolean coverageRun) throws CoreException {
        if (isUnittest()) {

            //The tests are either written to a configuration file or passed as a parameter.
            String configurationFile = this.configuration.getAttribute(Constants.ATTR_UNITTEST_CONFIGURATION_FILE, "");
            if (configurationFile.length() > 0) {
                cmdArgs.add("--config_file");
                if (actualRun) {
                    //We should write the contents to a temporary file (because it may be too long, so, always write
                    //to a file and read from it later on).
                    File tempFile = PydevPlugin.getDefault().getTempFile("custom_pydev_unittest_launch_");
                    try {
                        OutputStream fileOutputStream = new FileOutputStream(tempFile);
                        try {
                            try {
                                fileOutputStream.write(configurationFile.getBytes());
                            } catch (IOException e) {
                                throw new CoreException(SharedCorePlugin.makeStatus(IStatus.ERROR, "Error writing to: "
                                        + tempFile, e));
                            }
                        } finally {
                            fileOutputStream.close();
                        }
                    } catch (Exception e) {
                        if (e instanceof CoreException) {
                            throw (CoreException) e;
                        }
                        throw new CoreException(
                                SharedCorePlugin.makeStatus(IStatus.ERROR, "Error writing to: " + tempFile,
                                        e));
                    }
                    cmdArgs.add(tempFile.toString());
                } else {
                    cmdArgs.add(configurationFile);
                }
            } else {
                String tests = this.configuration.getAttribute(Constants.ATTR_UNITTEST_TESTS, "");
                if (tests.length() > 0) {
                    cmdArgs.add("--tests");
                    cmdArgs.add(tests);
                }
            }

            if (PyUnitPrefsPage2.getUsePyUnitView(project)) {
                //If we want to use the PyUnitView, we need to get the port used so that the python side can connect.
                cmdArgs.add("--port");
                if (actualRun) {
                    cmdArgs.add(String.valueOf(getPyUnitServer().getPort()));
                } else {
                    cmdArgs.add("0");
                }
            }

            if (coverageRun) {
                cmdArgs.add("--coverage_output_dir");
                cmdArgs.add(PyCoverage.getCoverageDirLocation().getAbsolutePath());

                cmdArgs.add("--coverage_include");
                cmdArgs.add(PyCodeCoverageView.getChosenDir().getLocation().toOSString());

                if (actualRun) {
                    int testRunner = PyUnitPrefsPage2.getTestRunner(this.configuration, project);

                    switch (testRunner) {
                        case PyUnitPrefsPage2.TEST_RUNNER_NOSE:
                            RunInUiThread.async(new Runnable() {

                                @Override
                                public void run() {
                                    PyDialogHelpers
                                            .openWarningWithIgnoreToggle(
                                                    "Notes for coverage with the nose test runner.",

                                                    "Note1: When using the coverage with the nose test runner, "
                                                            + "please don't pass any specific parameter related to "
                                                            + "the run in the arguments, as that's already handled by PyDev "
                                                            + "(i.e.: don't use the builtin cover plugin from nose).\n"
                                                            + "\n"
                                                            + "Note2: It's currently not possible to use coverage with the multi-process "
                                                            + "plugin in nose.",

                                                    "KEY_COVERAGE_WITH_NOSE_TEST_RUNNER");
                                }
                            });

                            break;
                        case PyUnitPrefsPage2.TEST_RUNNER_PY_TEST:
                            // Now works (using pytest-cov)
                            break;
                    }
                }
            }

            //Last thing: nose parameters or parameters the user configured.
            for (String s : ProcessUtils.parseArguments(PyUnitPrefsPage2.getTestRunnerParameters(this.configuration,
                    this.project))) {
                cmdArgs.add(s);
            }
        }
    }

    /**
     * Adds a set of arguments needed for debugging.
     * @param modName
     * @param addWithDashMFlag
     */
    private void addDebugArgs(List<String> cmdArgs, String vmType, boolean actualRun, boolean addWithDashMFlag,
            String modName) throws CoreException {
        if (isDebug) {
            cmdArgs.add(getDebugScript());
            if (DebugPrefsPage.getDebugMultiprocessingEnabled()) {
                cmdArgs.add("--multiprocess");
            }

            cmdArgs.add("--print-in-debugger-startup");

            String qtThreadsDebugMode = DebugPrefsPage.getQtThreadsDebugMode();
            if (qtThreadsDebugMode != null && qtThreadsDebugMode.length() > 0 && !qtThreadsDebugMode.equals("none")) {
                switch (qtThreadsDebugMode) {
                    case "auto":
                    case "pyqt5":
                    case "pyqt4":
                    case "pyside":
                        cmdArgs.add("--qt-support=" + qtThreadsDebugMode);
                        break;
                }
            }

            cmdArgs.add("--vm_type");
            cmdArgs.add(vmType);
            cmdArgs.add("--client");
            cmdArgs.add(LocalHost.getLocalHost());
            cmdArgs.add("--port");
            if (actualRun) {
                try {
                    cmdArgs.add(Integer.toString(getDebuggerListenConnector().getLocalPort()));
                } catch (IOException e) {
                    throw new CoreException(SharedCorePlugin.makeStatus(IStatus.ERROR, "Unable to get port", e));
                }
            } else {
                cmdArgs.add("0");
            }
            if (addWithDashMFlag) {
                cmdArgs.add("--module");
                cmdArgs.add("--file");
                cmdArgs.add(modName);

            } else {
                cmdArgs.add("--file");
            }
        }
    }

    /**
     * @param cmdArgs
     * @throws CoreException
     */
    private void addVmArgs(List<String> cmdArgs) throws CoreException {
        String[] vmArguments = getVMArguments(configuration);
        if (vmArguments != null) {
            for (int i = 0; i < vmArguments.length; i++) {
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
            String expanded = getStringSubstitution(PythonNature.getPythonNature(project)).performStringSubstitution(
                    args);
            return ProcessUtils.parseArguments(expanded);
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
            // append test names to command line to show
            String testArgs = configuration.getAttribute(Constants.ATTR_UNITTEST_TESTS, "");
            if (testArgs != "") {
                // only in case any tests were selected
                String[] argsWithTests = new String[args.length + 1];
                System.arraycopy(args, 0, argsWithTests, 0, args.length);
                argsWithTests[args.length] = testArgs;
                args = argsWithTests;
            }
            return SimpleRunner.getArgumentsAsStr(args);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public IInterpreterManager getRelatedInterpreterManager() {
        if (isJython()) {
            return InterpreterManagersAPI.getJythonInterpreterManager();
        }
        if (isIronpython()) {
            return InterpreterManagersAPI.getIronpythonInterpreterManager();
        }
        return InterpreterManagersAPI.getPythonInterpreterManager();
    }

    public PyUnitServer getPyUnitServer() {
        return this.pyUnitServer;
    }

    public synchronized ListenConnector getDebuggerListenConnector() throws IOException {
        if (this.listenConnector == null) {
            this.listenConnector = new ListenConnector(this.acceptTimeout);
        }
        return this.listenConnector;
    }

    public ILaunchConfiguration getLaunchConfiguration() {
        return this.configuration;
    }

    public PyUnitServer createPyUnitServer(PythonRunnerConfig config, ILaunch launch) throws IOException {
        if (this.pyUnitServer != null) {
            throw new AssertionError("PyUnitServer already created!");
        }
        this.pyUnitServer = new PyUnitServer(config, launch);
        return this.pyUnitServer;
    }

}
