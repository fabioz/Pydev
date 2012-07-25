/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 21, 2006
 */
package org.python.pydev.core;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public interface IInterpreterInfo {

    /**
     * @return a String such as 2.5 or 2.4 representing the python version that created this interpreter. 
     */
    public String getVersion();

    /**
     * @return a constant as defined in IGrammarVersionProvider.
     */
    public int getGrammarVersion();

    /**
     * @return a list of strings representing the pythonpath for this interpreter info.
     */
    public List<String> getPythonPath();

    public ISystemModulesManager getModulesManager();

    /**
     * @return a valid path for the interpreter (may not be human readable)
     */
    public String getExeAsFileSystemValidPath();

    /**
     * @return the environment variables that should be set when running this interpreter.
     * It can be null if the default environment should be used.
     */
    public String[] getEnvVariables();

    /**
     * This method receives the environment variables available for a run and updates them with the environment
     * variables that are contained in this interpreter.
     * 
     * Note that if a key already exists in the passed env and in the env contained for this interpreter, it's overridden
     * unless it's specified in keysThatShouldNotBeUpdated (which may be null). 
     */
    public String[] updateEnv(String[] env, Set<String> keysThatShouldNotBeUpdated);

    /**
     * Same as updateEnv(env, null)
     */
    public String[] updateEnv(String[] env);

    /**
     * Creates a copy of the current interpreter info (shares no variables with the original interpreter info). 
     */
    IInterpreterInfo makeCopy();

    /**
     * @return the executable or jar that this interpreter has (must be the jython.jar or python.exe)
     */
    public String getExecutableOrJar();

    /**
     * @return The name to be shown for the user for this interpreter info.
     */
    public String getName();

    /**
     * Sets the name to be shown for the user for this interpreter info.
     */
    public void setName(String name);

    /**
     * @return a suitable name to be shown in the UI (e.g: name + executable or jar)
     */
    public String getNameForUI();

    /**
     * @return true if the passed name matches the name or the executable or jar specified.
     */
    public boolean matchNameBackwardCompatible(String interpreter);

    /**
     * @return an iterator that can traverse the forced builtins;
     */
    public Iterator<String> forcedLibsIterator();

    public Properties getStringSubstitutionVariables();

    public List<String> getPredefinedCompletionsPath();

    /**
     * 
     */
    public void stopBuilding();

    /**
     * 
     */
    public void startBuilding();
}
