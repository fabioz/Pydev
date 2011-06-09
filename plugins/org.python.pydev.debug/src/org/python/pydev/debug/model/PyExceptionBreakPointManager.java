/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ListenerList;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;
import org.python.pydev.ui.interpreters.ChooseInterpreterManager;

public class PyExceptionBreakPointManager {

    //Static variables
    private static final String EXCEPTION_FILE_NAME = "python_exceptions.prefs";
    private static final String CUSTOM_EXCEPTION_FILE_NAME = "custom_exceptions.prefs";
    private static final String BREAK_ON_CAUGHT_EXCEPTION = "caught_exception_state.prefs";
    private static final String BREAK_ON_UNCAUGHT_EXCEPTION = "uncaught_exception_state.prefs";
    
    
    private static PyExceptionBreakPointManager pyExceptionBreakPointManager;
    private static final Object lock = new Object();

    
    //For instance
    private ListenerList<IExceptionsBreakpointListener> listeners = new ListenerList<IExceptionsBreakpointListener>(
            IExceptionsBreakpointListener.class);

    /**
     * Singleton: private constructor.
     */
    private PyExceptionBreakPointManager() {

    }
    
    public static PyExceptionBreakPointManager getInstance() {
        if (pyExceptionBreakPointManager == null) {
            synchronized (lock) {
                if (pyExceptionBreakPointManager == null) {
                    pyExceptionBreakPointManager = new PyExceptionBreakPointManager();
                }
            }
        }
        return pyExceptionBreakPointManager;
    }

    
    //Listeners
    
    public void addListener(IExceptionsBreakpointListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(IExceptionsBreakpointListener listener) {
        this.listeners.remove(listener);
    }
    

    //Setters
    
    
    /**
     * Sets whether we should break on caught/uncaught exceptions and the array of exceptions to be used.
     */
    public void setBreakOn(boolean breakOnCaught, boolean breakOnUncaught, String[] exceptionArray) {
        ConfigureExceptionsFileUtils.writeToFile(BREAK_ON_CAUGHT_EXCEPTION, Boolean.toString(breakOnCaught), false);
        
        ConfigureExceptionsFileUtils.writeToFile(BREAK_ON_UNCAUGHT_EXCEPTION, Boolean.toString(breakOnUncaught), false);
        
        String pyExceptionsStr = StringUtils.join(ConfigureExceptionsFileUtils.DELIMITER, exceptionArray);
        
        ConfigureExceptionsFileUtils.writeToFile(EXCEPTION_FILE_NAME, pyExceptionsStr, false);
        
        for (IExceptionsBreakpointListener listener : this.listeners.getListeners()) {
            listener.onSetConfiguredExceptions();
        }
    }

    
    /**
     * Adds a new custom exception the user entered (note that it just adds it to the list
     * of custom exceptions, it doesn't really change the exceptions set). 
     */
    public void addUserConfiguredException(String userConfiguredException) {
        boolean isAppend = false;

        IPath path = ConfigureExceptionsFileUtils.getFilePathFromMetadata(CUSTOM_EXCEPTION_FILE_NAME);
        if (path.toFile().exists()) {
            isAppend = true;
            userConfiguredException = ConfigureExceptionsFileUtils.DELIMITER + userConfiguredException;
        }
        ConfigureExceptionsFileUtils.writeToFile(CUSTOM_EXCEPTION_FILE_NAME, userConfiguredException, isAppend);
    }

    
    
    //Getters

    public String getBreakOnUncaughtExceptions() {
        return ConfigureExceptionsFileUtils.readFromMetadataFile(BREAK_ON_UNCAUGHT_EXCEPTION);
    }

    public String getBreakOnCaughtExceptions() {
        return ConfigureExceptionsFileUtils.readFromMetadataFile(BREAK_ON_CAUGHT_EXCEPTION);
    }

    public String getExceptionsString() {
        return ConfigureExceptionsFileUtils.readFromMetadataFile(EXCEPTION_FILE_NAME);
    }
    
    public List<String> getExceptionsList() {
        return ConfigureExceptionsFileUtils.getConfiguredExceptions(EXCEPTION_FILE_NAME);
    }


    /**
     * @return only the exceptions configured by the user (i.e.: not builtin exceptions)
     */
    public List<String> getUserConfiguredExceptions() {
        List<String> configuredExceptions = ConfigureExceptionsFileUtils.getConfiguredExceptions(CUSTOM_EXCEPTION_FILE_NAME);
        Collections.sort(configuredExceptions);

        return configuredExceptions;
    }


    /**
     * @return a list the default 'builtin' exceptions to be presented to the user (i.e.:
     * AssertionError, RuntimeError, etc)
     */
    public List<String> getBuiltinExceptions() {
        ArrayList<String> list = new ArrayList<String>();
        IInterpreterManager useManager = ChooseInterpreterManager.chooseInterpreterManager();
        if (useManager != null) {
            IToken[] pythonTokens = useManager.getBuiltinMod(IPythonNature.DEFAULT_INTERPRETER).getGlobalTokens();
            for (IToken token : pythonTokens) {
                String pyToken = token.getRepresentation();
                String lower = pyToken.toLowerCase();
                if (lower.contains("error") || lower.contains("exception") || lower.contains("warning")) {
                    list.add(pyToken.trim());
                }
            }
            Collections.sort(list);
        }
        return list;
    }



}
