/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.resource.StringConverter;
import org.osgi.service.prefs.Preferences;

/**
 * Initializes the preferences for the console.
 */
public class PydevConsolePreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode("org.python.pydev.debug");

        //text
        node.put(PydevConsoleConstants.PREF_CONTINUE_PROMPT, PydevConsoleConstants.DEFAULT_CONTINUE_PROMPT);
        node.put(PydevConsoleConstants.PREF_NEW_PROMPT, PydevConsoleConstants.DEFAULT_NEW_PROMPT);
        
        node.put(PydevConsoleConstants.CONSOLE_INPUT_COLOR, 
                StringConverter.asString(PydevConsoleConstants.DEFAULT_CONSOLE_SYS_IN_COLOR));
        
        node.put(PydevConsoleConstants.CONSOLE_OUTPUT_COLOR, 
                StringConverter.asString(PydevConsoleConstants.DEFAULT_CONSOLE_SYS_OUT_COLOR));
        
        node.put(PydevConsoleConstants.CONSOLE_ERROR_COLOR, 
                StringConverter.asString(PydevConsoleConstants.DEFAULT_CONSOLE_SYS_ERR_COLOR));
        
        node.put(PydevConsoleConstants.CONSOLE_PROMPT_COLOR, 
                StringConverter.asString(PydevConsoleConstants.DEFAULT_CONSOLE_PROMPT_COLOR));
        
        node.put(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR, 
                StringConverter.asString(PydevConsoleConstants.DEFAULT_CONSOLE_BACKGROUND_COLOR));
        
        
        node.put(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS, PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS);
        node.put(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS, PydevConsoleConstants.DEFAULT_INITIAL_INTERPRETER_CMDS);
        
        node.putInt(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS, 
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);
        
        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START, 
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);
        
        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND, 
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);
        
        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW, 
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW);
        
        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR, 
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);
    }

}
