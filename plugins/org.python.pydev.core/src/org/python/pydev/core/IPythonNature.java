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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

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

        public static final String JYTHON_VERSION_LATEST = JYTHON_VERSION_3_0;
        public static final String PYTHON_VERSION_LATEST = PYTHON_VERSION_3_12;
        public static final String IRONPYTHON_VERSION_LATEST = IRONPYTHON_VERSION_3_0;

        public static final String INTERPRETER_VERSION = "interpreter";
        private static final Map<String, String> mappedVersions = new HashMap<>();

        public static final String PYTHON_PREFIX = "python";
        public static final String JYTHON_PREFIX = "jython";
        public static final String IRONYTHON_PREFIX = "ironpython";

        public static final String LATEST_VERSION_NUMBER = "3.12";

        static {
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_0);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_6);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_7);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_8);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_9);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_10);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_11);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_12);
            ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_INTERPRETER);

            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_0);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_6);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_7);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_8);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_9);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_10);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_11);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_12);
            ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_INTERPRETER);

            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_0);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_6);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_7);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_8);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_9);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_10);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_11);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_12);
            ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_INTERPRETER);

            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_JYTHON_VERSIONS);
            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_PYTHON_VERSIONS);
            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_IRONPYTHON_VERSIONS);

            VERSION_NUMBERS.add("3.5");
            VERSION_NUMBERS.add("3.6");
            VERSION_NUMBERS.add("3.7"); // actually the same as 3.6
            VERSION_NUMBERS.add("3.8");
            VERSION_NUMBERS.add("3.9"); // actually the same as 3.8
            VERSION_NUMBERS.add("3.10");
            VERSION_NUMBERS.add("3.11");
            VERSION_NUMBERS.add("3.12");
            VERSION_NUMBERS.add(INTERPRETER_VERSION);

            // Anything below 3.5 is marked as 3.5
            mappedVersions.put("3.0", "3.5");
            mappedVersions.put("3.1", "3.5");
            mappedVersions.put("3.2", "3.5");
            mappedVersions.put("3.3", "3.5");
            mappedVersions.put("3.4", "3.5");
            mappedVersions.put("3.5", "3.5");

            mappedVersions.put("3.6", "3.6");

            mappedVersions.put("3.7", "3.7"); // actually the same as 3.6

            mappedVersions.put("3.8", "3.8");

            mappedVersions.put("3.9", "3.9"); // actually the same as 3.8

            mappedVersions.put("3.10", "3.10");

            mappedVersions.put("3.11", "3.11");

            mappedVersions.put("3.12", "3.12");
        }

        /**
         * @param interpreterType
         * @param version
         * @return
         */
        public static String convertToInternalVersion(int interpreterType, final String initialVersion) {
            FastStringBuffer buf;
            switch (interpreterType) {
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    buf = new FastStringBuffer("python ", 5);
                    break;

                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    buf = new FastStringBuffer("jython ", 5);
                    break;

                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    buf = new FastStringBuffer("ironpython ", 5);
                    break;

                default:
                    throw new RuntimeException("Not Python nor Jython nor IronPython?");
            }

            return convertToInternalVersion(buf, initialVersion);
        }

        public static String convertToInternalVersion(FastStringBuffer buf, final String initialVersion)
                throws AssertionError {
            String version = mappedVersions.get(initialVersion);
            if (version != null) {
                buf.append(version);
                String fullVersion = buf.toString();
                if (ALL_VERSIONS_ANY_FLAVOR.contains(fullVersion)) {
                    return fullVersion;
                }
                throw new AssertionError(
                        "Should never get here (wrong version mapping). Initial version: " + initialVersion);
            }

            // It's a version we don't know about (there's no direct mapping for it).

            version = initialVersion;
            Assert.isTrue(!INTERPRETER_VERSION.equals(version),
                    "The actual version must be passed, cannot be 'interpreter' at this point.");
            if (version.startsWith("2.") || version.startsWith("1.")) {
                throw new RuntimeException("\n\nPython:" + version
                        + " is no longer supported.\n\nPlease refer to: Need to use older Eclipse/Java/Python\n\nin https://www.pydev.org/download.html\n\n");
            }
            // It seems a version we don't directly support, let's check it...
            Log.log("Unsupported version:" + version + " (falling back to " + LATEST_VERSION_NUMBER + ")");
            buf.append(LATEST_VERSION_NUMBER); // latest 3
            String fullVersion = buf.toString();
            if (ALL_VERSIONS_ANY_FLAVOR.contains(fullVersion)) {
                return fullVersion;
            }
            throw new AssertionError("Should never get here. Initial version: " + initialVersion);
        }

        private static List<Integer> grammarVersions = GrammarsIterator.createList();

        private static Map<String, Integer> grammarRepToVersion = GrammarsIterator.createStrToInt();

        public static List<Integer> getSupportedInternalGrammarVersions() {
            return grammarVersions;
        }

        /**
         * @param version Just "3.0", "2.7", etc.
         */
        public static boolean supportsVersion(String version) {
            return mappedVersions.containsKey(version);
        }

        /**
         * @param version Just "3.0", "2.7", etc.
         */
        public static int getInternalVersion(String version) {
            return grammarRepToVersion.get(mappedVersions.get(version));
        }
    }

    /**
     * Constants persisted. Probably a better way would be disassociating whether it's python/jython and the
     * grammar version to be used (to avoid the explosion of constants below).
     */
    public static final String PYTHON_VERSION_3_0 = "python 3.5";
    public static final String PYTHON_VERSION_3_6 = "python 3.6";
    public static final String PYTHON_VERSION_3_7 = "python 3.7";
    public static final String PYTHON_VERSION_3_8 = "python 3.8";
    public static final String PYTHON_VERSION_3_9 = "python 3.9";
    public static final String PYTHON_VERSION_3_10 = "python 3.10";
    public static final String PYTHON_VERSION_3_11 = "python 3.11";
    public static final String PYTHON_VERSION_3_12 = "python 3.12";
    public static final String PYTHON_VERSION_INTERPRETER = "python interpreter";

    public static final String JYTHON_VERSION_3_0 = "jython 3.5";
    public static final String JYTHON_VERSION_3_6 = "jython 3.6";
    public static final String JYTHON_VERSION_3_7 = "jython 3.7";
    public static final String JYTHON_VERSION_3_8 = "jython 3.8";
    public static final String JYTHON_VERSION_3_9 = "jython 3.9";
    public static final String JYTHON_VERSION_3_10 = "jython 3.10";
    public static final String JYTHON_VERSION_3_11 = "jython 3.11";
    public static final String JYTHON_VERSION_3_12 = "jython 3.12";
    public static final String JYTHON_VERSION_INTERPRETER = "jython interpreter";

    public static final String IRONPYTHON_VERSION_3_0 = "ironpython 3.5";
    public static final String IRONPYTHON_VERSION_3_6 = "ironpython 3.6";
    public static final String IRONPYTHON_VERSION_3_7 = "ironpython 3.7";
    public static final String IRONPYTHON_VERSION_3_8 = "ironpython 3.8";
    public static final String IRONPYTHON_VERSION_3_9 = "ironpython 3.9";
    public static final String IRONPYTHON_VERSION_3_10 = "ironpython 3.10";
    public static final String IRONPYTHON_VERSION_3_11 = "ironpython 3.11";
    public static final String IRONPYTHON_VERSION_3_12 = "ironpython 3.12";
    public static final String IRONPYTHON_VERSION_INTERPRETER = "ironpython interpreter";

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
     * @return the project version given the constants provided (something as "python 2.7" or "jython 3.0").
     *
     * Unfortunately the version includes the interpreter type... ideally we'd undo this to have only the version, but
     * it's a bit too much work at this point to do that.
     */
    String getVersion(boolean translateIfInterpreter) throws CoreException;

    /**
     * @return the project version given the constants provided (something as "python 2.7" or "jython 3.0") along with an error to be shown to the user.
     *
     * Unfortunately the version includes the interpreter type... ideally we'd undo this to have only the version, but
     * it's a bit too much work at this point to do that.
     */
    Tuple<String, String> getVersionAndError(boolean translateIfInterpreter) throws CoreException;

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
    TokensList getBuiltinCompletions(IModuleRequestState moduleRequest);

    /**
     * @param toks those are the tokens that are set as builtin completions.
     */
    void clearBuiltinCompletions();

    /**
     * @return the module for the builtins (may return null if not set)
     */
    IModule getBuiltinMod(IModuleRequestState moduleRequest);

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

    void updateMtime();

    long getMtime();

}
