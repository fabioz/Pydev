/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 21/08/2005
 */
package org.python.pydev.core;

import org.python.pydev.shared_core.utils.BaseExtensionHelper;

public class ExtensionHelper extends BaseExtensionHelper {

    //pydev
    public final static String PYDEV_COMPLETION = "org.python.pydev.pydev_completion";
    public final static String PYDEV_BUILDER = "org.python.pydev.pydev_builder";
    public final static String PYDEV_MODULES_OBSERVER = "org.python.pydev.pydev_modules_observer";
    public final static String PYDEV_VIEW_CREATED_OBSERVER = "org.python.pydev.pydev_view_created_observer";
    public final static String PYDEV_INTERPRETER_OBSERVER = "org.python.pydev.pydev_interpreter_observer";
    public final static String PYDEV_INTERPRETER_NEW_CUSTOM_ENTRIES = "org.python.pydev.pydev_interpreter_new_custom_entries";
    public final static String PYDEV_MANAGER_OBSERVER = "org.python.pydev.pydev_manager_observer";
    public final static String PYDEV_PARSER_OBSERVER = "org.python.pydev.parser.pydev_parser_observer";
    public static final String PYDEV_CTRL_1 = "org.python.pydev.pydev_ctrl_1";
    public static final String PYDEV_SIMPLE_ASSIST = "org.python.pydev.pydev_simpleassist";
    public static final String PYDEV_ORGANIZE_IMPORTS = "org.python.pydev.pydev_organize_imports";
    public static final String PYDEV_REFACTORING = "org.python.pydev.pydev_refactoring";
    public static final String PYDEV_QUICK_OUTLINE = "org.python.pydev.pydev_quick_outline";
    public static final String PYDEV_PYEDIT_LISTENER = "org.python.pydev.pydev_pyedit_listener";
    public static final String PYDEV_FORMATTER = "org.python.pydev.pydev_formatter";
    public static final String PYDEV_GLOBALS_BROWSER = "org.python.pydev.pydev_globals_browser";
    public static final String PYDEV_DEBUG_PREFERENCES_PAGE = "org.python.pydev.pydev_debug_preferences_page";
    public static final String PYDEV_HOVER = "org.python.pydev.pydev_hover";

    //IInterpreterInfoBuilder
    public static final String PYDEV_INTERPRETER_INFO_BUILDER = "org.python.pydev.pydev_interpreter_info_builder";

    //IInterpreterProviderFactory
    public static final String PYDEV_INTERPRETER_PROVIDER = "org.python.pydev.pydev_interpreter_provider";

    //debug
    public static final String PYDEV_DEBUG_CONSOLE_INPUT_LISTENER = "org.python.pydev.debug.pydev_debug_console_input_listener";
    public static final String PYDEV_COMMAND_LINE_PARTICIPANT = "org.python.pydev.debug.pydev_debug_command_line_participant";

    // Module resolver
    public static final String PYDEV_PYTHON_MODULE_RESOLVER = "org.python.pydev.pydev_python_module_resolver";
}
