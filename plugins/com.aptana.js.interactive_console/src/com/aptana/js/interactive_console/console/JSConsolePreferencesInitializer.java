/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.resource.StringConverter;
import org.osgi.service.prefs.Preferences;

/**
 * Initializes the preferences for the console.
 */
public class JSConsolePreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode("com.aptana.js.interactive_console");

        //text
        node.put(JSConsoleConstants.PREF_CONTINUE_PROMPT, JSConsoleConstants.DEFAULT_CONTINUE_PROMPT);
        node.put(JSConsoleConstants.PREF_NEW_PROMPT, JSConsoleConstants.DEFAULT_NEW_PROMPT);

        node.put(JSConsoleConstants.CONSOLE_INPUT_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_CONSOLE_SYS_IN_COLOR));

        node.put(JSConsoleConstants.CONSOLE_OUTPUT_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_CONSOLE_SYS_OUT_COLOR));

        node.put(JSConsoleConstants.CONSOLE_ERROR_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_CONSOLE_SYS_ERR_COLOR));

        node.put(JSConsoleConstants.CONSOLE_PROMPT_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_CONSOLE_PROMPT_COLOR));

        node.put(JSConsoleConstants.CONSOLE_BACKGROUND_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_CONSOLE_BACKGROUND_COLOR));

        node.put(JSConsoleConstants.DEBUG_CONSOLE_BACKGROUND_COLOR,
                StringConverter.asString(JSConsoleConstants.DEFAULT_DEBUG_CONSOLE_BACKGROUND_COLOR));

        node.put(JSConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS,
                JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS);
        node.put(JSConsoleConstants.INITIAL_INTERPRETER_CMDS, JSConsoleConstants.DEFAULT_INITIAL_INTERPRETER_CMDS);

        node.putInt(JSConsoleConstants.INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS,
                JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS);

        node.putBoolean(JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START,
                JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START);

        node.putBoolean(JSConsoleConstants.INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND,
                JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND);

        node.putBoolean(JSConsoleConstants.INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR,
                JSConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR);
    }

}
