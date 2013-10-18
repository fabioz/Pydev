/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public interface IInterpreterManager {

    /**
     * This is the constant from where we get the interpreter info
     */
    public String PYTHON_INTERPRETER_PATH = "INTERPRETER_PATH_NEW";

    /**
     * This is the constant from where we get the ipy.exe for iron python
     */
    public String IRONPYTHON_INTERPRETER_PATH = "IRONPYTHON_INTERPRETER_PATH";

    /**
     * This is the constant from where we get the jython jar
     */
    public String JYTHON_INTERPRETER_PATH = "JYTHON_INTERPRETER_PATH";

    /**
     * This is the constant from where we get the jython cache dir
     */
    public String JYTHON_CACHE_DIR = "JYTHON_CACHE_DIR";

    /**
     * This is the constant from where we get the default vm args for Iron Python
     */
    public String IRONPYTHON_INTERNAL_SHELL_VM_ARGS = "IRONPYTHON_INTERNAL_SHELL_VM_ARGS";

    /**
     * Constant for the default values
     */
    public String IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS = "-X:Frames";

    public int INTERPRETER_TYPE_PYTHON = IPythonNature.INTERPRETER_TYPE_PYTHON;
    public int INTERPRETER_TYPE_JYTHON = IPythonNature.INTERPRETER_TYPE_JYTHON;
    public int INTERPRETER_TYPE_IRONPYTHON = IPythonNature.INTERPRETER_TYPE_IRONPYTHON;
    public int INTERPRETER_TYPE_JYTHON_ECLIPSE = IPythonNature.INTERPRETER_TYPE_JYTHON_ECLIPSE;

    /**
     * @return the default interpreter for the given interpreter manager.
     */
    public IInterpreterInfo getDefaultInterpreterInfo(boolean autoConfigureIfNotConfigured)
            throws MisconfigurationException;

    /**
     * @return the interpreter infos kept internally in the interpreter manager.
     */
    public IInterpreterInfo[] getInterpreterInfos();

    /**
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     *
     * @param executable this is the executable from where we want to get the info
     * @return information on the executable
     */
    public IInterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor)
            throws MisconfigurationException;

    /**
     * This function should be used to create the interpreter info of some executable.
     *
     * @param executable interpreter for which the info should be created.
     * @param monitor
     * @return the executable gotten (it could be different from the input, because we could receive a link and
     * return the actual executable in the system).
     * @throws JDTNotAvailableException
     * @throws CoreException
     */
    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser);

    /**
     * @param persisted string previously persisted
     * @return list of interpreter infos
     */
    public IInterpreterInfo[] getInterpretersFromPersistedString(String persisted);

    /**
     * All the information cached should be cleared but the information related to the passed interpreters
     * @param interpreterNamesToRestore
     */
    public void setInfos(IInterpreterInfo[] infos, Set<String> interpreterNamesToRestore, IProgressMonitor monitor);

    /**
     * @return the name that is related to this manager (e.g.: python, jython...)
     */
    String getManagerRelatedName();

    /**
     * @return the Persisted string with the information on this interpreter manager.
     */
    public String getPersistedString();

    /**
     * @return whether this manager is correctly configured (interpreter is correctly set)
     */
    public boolean isConfigured();

    /**
     * @return IPythonNature.INTERPRETER_TYPE_PYTHON or
     *         IPythonNature.INTERPRETER_TYPE_JYTHON or
     *         IPythonNature.INTERPRETER_TYPE_IRONPYTHON
     */
    public int getInterpreterType();

    //caches for the builtin tokens and module
    public void clearBuiltinCompletions(String projectInterpreterName);

    public IToken[] getBuiltinCompletions(String projectInterpreterName);

    public IModule getBuiltinMod(String projectInterpreterName);

    public void clearBuiltinMod(String projectInterpreterName);

    public void clearCaches();

    public void addListener(IInterpreterManagerListener listener);
}
