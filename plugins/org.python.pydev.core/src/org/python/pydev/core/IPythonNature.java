/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * @author Fabio
 */
public interface IPythonNature extends IProjectNature, IGrammarVersionProvider, IAdaptable {

    /**
     * Helper class to contain information about the versions
     */
    public static class Versions {
        public static final HashSet<String> ALL_PYTHON_VERSIONS = new HashSet<String>();
        public static final HashSet<String> ALL_JYTHON_VERSIONS = new HashSet<String>();
        public static final HashSet<String> ALL_IRONPYTHON_VERSIONS = new HashSet<String>();
        public static final HashSet<String> ALL_VERSIONS_ANY_FLAVOR = new HashSet<String>();
        public static final List<String> VERSION_NUMBERS = new ArrayList<String>();
        public static final String LAST_VERSION_NUMBER = "2.7";

        static {
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_1);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_2);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_3);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_4);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_5);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_6);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_7);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_0);

            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_1);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_2);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_3);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_4);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_5);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_6);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_7);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_0);

            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_1);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_2);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_3);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_4);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_5);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_6);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_7);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_0);

            VERSION_NUMBERS.add("2.1");
            VERSION_NUMBERS.add("2.2");
            VERSION_NUMBERS.add("2.3");
            VERSION_NUMBERS.add("2.4");
            VERSION_NUMBERS.add("2.5");
            VERSION_NUMBERS.add("2.6");
            VERSION_NUMBERS.add("2.7");
            VERSION_NUMBERS.add("3.0");

            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_JYTHON_VERSIONS);
            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_PYTHON_VERSIONS);
            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_IRONPYTHON_VERSIONS);
        }
    }

    /**
     * Constants persisted. Probably a better way would be disassociating whether it's python/jython and the 
     * grammar version to be used (to avoid the explosion of constants below).
     */
    public static final String PYTHON_VERSION_2_1 = "python 2.1";
    public static final String PYTHON_VERSION_2_2 = "python 2.2";
    public static final String PYTHON_VERSION_2_3 = "python 2.3";
    public static final String PYTHON_VERSION_2_4 = "python 2.4";
    public static final String PYTHON_VERSION_2_5 = "python 2.5";
    public static final String PYTHON_VERSION_2_6 = "python 2.6";
    public static final String PYTHON_VERSION_2_7 = "python 2.7";
    public static final String PYTHON_VERSION_3_0 = "python 3.0";

    public static final String JYTHON_VERSION_2_1 = "jython 2.1";
    public static final String JYTHON_VERSION_2_2 = "jython 2.2";
    public static final String JYTHON_VERSION_2_3 = "jython 2.3";
    public static final String JYTHON_VERSION_2_4 = "jython 2.4";
    public static final String JYTHON_VERSION_2_5 = "jython 2.5";
    public static final String JYTHON_VERSION_2_6 = "jython 2.6";
    public static final String JYTHON_VERSION_2_7 = "jython 2.7";
    public static final String JYTHON_VERSION_3_0 = "jython 3.0";

    public static final String IRONPYTHON_VERSION_2_1 = "ironpython 2.1";
    public static final String IRONPYTHON_VERSION_2_2 = "ironpython 2.2";
    public static final String IRONPYTHON_VERSION_2_3 = "ironpython 2.3";
    public static final String IRONPYTHON_VERSION_2_4 = "ironpython 2.4";
    public static final String IRONPYTHON_VERSION_2_5 = "ironpython 2.5";
    public static final String IRONPYTHON_VERSION_2_6 = "ironpython 2.6";
    public static final String IRONPYTHON_VERSION_2_7 = "ironpython 2.7";
    public static final String IRONPYTHON_VERSION_3_0 = "ironpython 3.0";

    //NOTE: It's the latest in the 2 series (3 is as if it's a totally new thing)
    public static final String JYTHON_VERSION_LATEST = JYTHON_VERSION_2_6;
    public static final String PYTHON_VERSION_LATEST = PYTHON_VERSION_2_7;

    /**
     * this id is provided so that we can have an identifier for python-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_PYTHON = 0;

    /**
     * this id is provided so that we can have an identifier for jython-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_JYTHON = 1;

    /**
     * This id is provided so that we can have an identifier for ironpython-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_IRONPYTHON = 2;

    /**
     * Identifies an interpreter that will use the jython in the running eclipse platform.
     */
    public static final int INTERPRETER_TYPE_JYTHON_ECLIPSE = 3;

    public static final String DEFAULT_INTERPRETER = "Default";

    /**
     * @return the project version given the constants provided
     * @throws CoreException 
     */
    String getVersion() throws CoreException;

    /**
     * @return the default version
     * @throws CoreException 
     */
    String getDefaultVersion();

    /**
     * set the project version given the constants provided
     * 
     * @see PYTHON_VERSION_XX
     * @see JYTHON_VERSION_XX
     * 
     * @throws CoreException 
     */
    void setVersion(String version, String interpreter) throws CoreException;

    /**
     * @return the id that is related to this nature given its type
     * 
     * @see #INTERPRETER_TYPE_PYTHON
     * @see #INTERPRETER_TYPE_JYTHON
     * @see #INTERPRETER_TYPE_IRONPYTHON
     */
    int getInterpreterType() throws CoreException;

    /**
     * @return the directory where the completions should be saved (as well as deltas)
     */
    public File getCompletionsCacheDir();

    /**
     * Saves the ast manager information so that we can retrieve it later.
     */
    public void saveAstManager();

    IPythonPathNature getPythonPathNature();

    String resolveModule(File file) throws MisconfigurationException;

    String resolveModule(String fileAbsolutePath) throws MisconfigurationException;

    String resolveModule(IResource resource) throws MisconfigurationException;

    String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException;

    String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException;

    ICodeCompletionASTManager getAstManager();

    /**
     * Rebuilds the path with the current path information (just to refresh it).
     * @throws CoreException 
     */
    void rebuildPath();

    /**
     * @return the interpreter manager that's related to the interpreter configured in this nature.
     */
    IInterpreterManager getRelatedInterpreterManager();

    /**
     * @return the tokens for the builtins. As getting the builtins is VERY usual, we'll keep them here.
     * (we can't forget to change it when the interpreter is changed -- on rebuildPath)
     * 
     * May return null if not set
     */
    IToken[] getBuiltinCompletions();

    /**
     * @param toks those are the tokens that are set as builtin completions.
     */
    void clearBuiltinCompletions();

    /**
     * @return the module for the builtins (may return null if not set)
     */
    IModule getBuiltinMod();

    void clearBuiltinMod();

    /**
     * Checks if the given resource is in the pythonpath
     * @throws MisconfigurationException 
     */
    boolean isResourceInPythonpath(IResource resource) throws MisconfigurationException;

    boolean isResourceInPythonpath(String resource) throws MisconfigurationException;

    boolean isResourceInPythonpathProjectSources(IResource fileAdapter, boolean addExternal)
            throws MisconfigurationException, CoreException;

    boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
            throws MisconfigurationException, CoreException;

    /**
     * @return true if it is ok to use the nature
     */
    boolean startRequests();

    void endRequests();

    /**
     * @return the configured interpreter that should be used to get the completions (must be the same string
     * of one of the configured interpreters in the preferences).
     * 
     * Must always be a valid path (e.g.: if the interpreter is internally configured as "Default", it should
     * return the actual path, not the internal representation).
     * 
     * Note: the return can never be null (an exception is thrown if none can be determined) 
     * @throws PythonNatureWithoutProjectException 
     */
    IInterpreterInfo getProjectInterpreter() throws MisconfigurationException, PythonNatureWithoutProjectException;

    boolean isOkToUse();

}
