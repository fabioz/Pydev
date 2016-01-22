/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import org.eclipse.swt.graphics.RGB;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;

/**
 * Constants for the console
 */
public final class PydevConsoleConstants {

    public static final String PREF_NEW_PROMPT = "pydevconsole_new_invitation";
    public static final String PREF_CONTINUE_PROMPT = "pydevconsole_continue_invitation";

    public static final String DEFAULT_NEW_PROMPT = ">>> ";
    public static final String DEFAULT_CONTINUE_PROMPT = "... ";

    public static final String CONSOLE_OUTPUT_COLOR = "pydevconsole_sysout_color";
    public static final RGB DEFAULT_CONSOLE_SYS_OUT_COLOR = new RGB(0, 0, 0);

    public static final String CONSOLE_ERROR_COLOR = "pydevconsole_syserr_color";
    public static final RGB DEFAULT_CONSOLE_SYS_ERR_COLOR = new RGB(255, 0, 0);

    public static final String CONSOLE_INPUT_COLOR = "pydevconsole_sysin_color";
    public static final RGB DEFAULT_CONSOLE_SYS_IN_COLOR = new RGB(0, 0, 255);

    public static final String CONSOLE_PROMPT_COLOR = "pydevconsole_prompt_color";
    public static final RGB DEFAULT_CONSOLE_PROMPT_COLOR = new RGB(0, 255, 0);

    public static final String CONSOLE_BACKGROUND_COLOR = "pydevconsole_background_color";
    public static final RGB DEFAULT_CONSOLE_BACKGROUND_COLOR = new RGB(255, 255, 255);

    public static final String DEBUG_CONSOLE_BACKGROUND_COLOR = "pydevdebugconsole_background_color";
    public static final RGB DEFAULT_DEBUG_CONSOLE_BACKGROUND_COLOR = new RGB(230, 230, 230); // Light Gray

    public static final String CONSOLE_TYPE = ScriptConsole.DEFAULT_CONSOLE_TYPE;
    public static final String DEBUG_CONSOLE_TYPE = "org.python.pydev.debug.newconsole.PydevDebugConsole";

    public static final String INTERACTIVE_CONSOLE_VM_ARGS = "INTERACTIVE_CONSOLE_VM_ARGS";
    public static final String DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS = "-Xmx64m";

    public static final String INTERACTIVE_CONSOLE_ENCODING = "INTERACTIVE_CONSOLE_ENCODING";
    public static final String DEFAULT_INTERACTIVE_CONSOLE_ENCODING = "UTF-8";

    public static final String INITIAL_INTERPRETER_CMDS = "INITIAL_INTERPRETER_CMDS";
    public static final String DEFAULT_INITIAL_INTERPRETER_CMDS = "import sys; print('%s %s' % (sys.executable or sys.platform, sys.version))\n";

    public static final String DJANGO_INTERPRETER_CMDS = "DJANGO_INTERPRETER_CMDS";
    public static final String DEFAULT_DJANGO_INTERPRETER_CMDS = "import os; os.environ['DJANGO_SETTINGS_MODULE'] = '${DJANGO_SETTINGS_MODULE}'; import django\nif django.get_version() < '1.5':\n\tfrom django.core import management\n\timport ${DJANGO_SETTINGS_MODULE} as settings\n\tmanagement.setup_environ(settings)\nif django.get_version() >= '1.7':\n\tfrom django.core.wsgi import get_wsgi_application\n\tapplication = get_wsgi_application()\n";

    public static final String INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS = "INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS";
    public static final int DEFAULT_INTERACTIVE_CONSOLE_MAXIMUM_CONNECTION_ATTEMPTS = 50;

    public static final String INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START = "INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_CONSOLE_START = true;

    public static final String INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND = "INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_FOCUS_ON_SEND_COMMAND = true;

    public static final String INTERACTIVE_CONSOLE_TAB_COMPLETION = "INTERACTIVE_CONSOLE_TAB_COMPLETION";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_TAB_COMPLETION = true;

    public static final String INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION = "INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_CONNECT_DEBUG_SESSION = false;

    public static final String INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR = "INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_SEND_INITIAL_COMMAND_WHEN_CREATED_FROM_EDITOR = false;

    public static final String INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP = "INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP";
    public static final String DEFAULT_INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP = "none";
    public static final String[][] ENTRIES_VALUES_INTERACTIVE_CONSOLE_ENABLE_GUI_ON_STARTUP = new String[][] {
            { "No GUI Enabled On Statup (none)", "none" },
            { "wxPython (wx)", "wx" },
            { "PyQt (qt/qt4)", "qt4" },
            { "PyGTK (gtk)", "gtk" },
            { "PyGI (gtk3)", "gtk3" },
            { "Tkinter (tk)", "tk" },
            { "OS X (osx)", "osx" },
            { "OpenGL (glut)", "glut" },
            { "pyglet (pyglet)", "pyglet" } };

    public static final String INTERACTIVE_CONSOLE_UMD_ENABLED = "INTERACTIVE_CONSOLE_UMD_ENABLED";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_UMD_ENABLED = true;

    public static final String INTERACTIVE_CONSOLE_UMD_VERBOSE = "INTERACTIVE_CONSOLE_UMD_VERBOSE";
    public static final boolean DEFAULT_INTERACTIVE_CONSOLE_UMD_VERBOSE = true;

    public static final String INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST = "INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST";
    public static final String DEFAULT_INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST = "guidata,guiqwt";

    public static final int CONSOLE_TIMEOUT = 500;

}
