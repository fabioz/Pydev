/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console;

import org.eclipse.swt.graphics.RGB;

/**
 * Constants for the console
 */
public final class JSConsoleConstants {

    public static final String PREF_NEW_PROMPT = "jsconsole_new_invitation";
    public static final String PREF_CONTINUE_PROMPT = "jsconsole_continue_invitation";

    public static final String DEFAULT_NEW_PROMPT = ">>> ";
    public static final String DEFAULT_CONTINUE_PROMPT = "... ";

    public static final String CONSOLE_OUTPUT_COLOR = "jsconsole_sysout_color";
    public static final RGB DEFAULT_CONSOLE_SYS_OUT_COLOR = new RGB(0, 0, 0);

    public static final String CONSOLE_ERROR_COLOR = "jsconsole_syserr_color";
    public static final RGB DEFAULT_CONSOLE_SYS_ERR_COLOR = new RGB(255, 0, 0);

    public static final String CONSOLE_INPUT_COLOR = "jsconsole_sysin_color";
    public static final RGB DEFAULT_CONSOLE_SYS_IN_COLOR = new RGB(0, 0, 255);

    public static final String CONSOLE_PROMPT_COLOR = "jsconsole_prompt_color";
    public static final RGB DEFAULT_CONSOLE_PROMPT_COLOR = new RGB(0, 255, 0);

    public static final String CONSOLE_BACKGROUND_COLOR = "jsconsole_background_color";
    public static final RGB DEFAULT_CONSOLE_BACKGROUND_COLOR = new RGB(255, 255, 255);

    public static final String DEBUG_CONSOLE_BACKGROUND_COLOR = "jsdebugconsole_background_color";
    public static final RGB DEFAULT_DEBUG_CONSOLE_BACKGROUND_COLOR = new RGB(230, 230, 230); // Light Gray

    public static final String CONSOLE_TYPE = "com.aptana.js.interactive_console.console.JSConsole";

    public static final String INTERACTIVE_CONSOLE_VM_ARGS = "INTERACTIVE_CONSOLE_VM_ARGS";
    public static final String DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS = "-Xmx64m";

    public static final String INITIAL_INTERPRETER_CMDS = "INITIAL_INTERPRETER_CMDS";
    public static final String DEFAULT_INITIAL_INTERPRETER_CMDS = "print(\"Console initialized\")\n";

    public static final String INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS = "INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS";
    public static final int DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS = 50;

    public static final String INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START = "INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START = true;

    public static final String INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND = "INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND = true;

    public static final String INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW = "INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_CONNECT_VARIABLE_VIEW = false;

    public static final String INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR = "INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR = true;

    public static final int CONSOLE_TIMEOUT = 500;

}
