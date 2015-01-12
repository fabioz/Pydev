/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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

        node.put(PydevConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR,
                StringConverter.asString(PydevConsoleConstants.DEFAULT_DEBUG_CONSOLE_BACKGROUND_COLOR));

        node.put(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS);

        node.put(PydevConsoleConstants.INTERACTIVE_CONSOLE_ENCODING,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_ENCODING);

        node.put(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS, PydevConsoleConstants.DEFAULT_INITIAL_INTERPRETER_CMDS);

        node.put(PydevConsoleConstants.DJANGO_INTERPRETER_CMDS, PydevConsoleConstants.DEFAULT_DJANGO_INTERPRETER_CMDS);

        node.putInt(PydevConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_TAB_COMPLETION,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_TAB_COMPLETION);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);

        node.put(PydevConsoleConstants.INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_ENABLED,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_ENABLED);

        node.putBoolean(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_VERBOSE,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_VERBOSE);

        node.put(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST,
                PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST);
    }

}
