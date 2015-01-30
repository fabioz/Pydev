/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.debug.model.PyVariablesPreferences;
import org.python.pydev.debug.pyunit.PyUnitView;

public class PydevDebugPreferencesInitializer extends AbstractPreferenceInitializer {

    /**
     * Note that this preference is currently nowhere for the user to edit.
     */
    public static final String HIDE_PYDEVD_THREADS = "HIDE_PYDEVD_THREADS";
    public static final boolean DEFAULT_HIDE_PYDEVD_THREADS = true;

    public static final String IGNORE_EXCEPTIONS_THROWN_IN_LINES_WITH_IGNORE_EXCEPTION = "IGNORE_EXCEPTIONS_THROWN_IN_LINES_WITH_IGNORE_EXCEPTION";
    public static final boolean DEFAULT_IGNORE_EXCEPTIONS_THROWN_IN_LINES_WITH_IGNORE_EXCEPTION = true;

    public static final String SKIP_CAUGHT_EXCEPTIONS_IN_SAME_FUNCTION = "SKIP_CAUGHT_EXCEPTIONS_IN_SAME_FUNCTION";
    public static final boolean DEFAULT_SKIP_CAUGHT_EXCEPTIONS_IN_SAME_FUNCTION = false;

    public static final String SHOW_CONSOLE_PROMPT_ON_DEBUG = "SHOW_CONSOLE_PROMPT_ON_DEBUG";
    public final static String RELATIVE_CONSOLE_HEIGHT = "RELATIVE_CONSOLE_HEIGHT";
    public final static String CONSOLE_PROMPT_OUTPUT_MODE = "CONSOLE_PROMPT_OUTPUT_MODE";
    public final static int MODE_ASYNC_SEPARATE_CONSOLE = 1;
    public final static int MODE_NOT_ASYNC_SAME_CONSOLE = 2;

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode("org.python.pydev.debug");

        //py unit view
        node.putBoolean(PyUnitView.PYUNIT_VIEW_SHOW_ONLY_ERRORS, PyUnitView.PYUNIT_VIEW_DEFAULT_SHOW_ONLY_ERRORS);
        node.putBoolean(PyUnitView.PYUNIT_VIEW_SHOW_VIEW_ON_TEST_RUN,
                PyUnitView.PYUNIT_VIEW_DEFAULT_SHOW_VIEW_ON_TEST_RUN);
        node.putBoolean(PyUnitView.PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS,
                PyUnitView.PYUNIT_VIEW_DEFAULT_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS);

        //debug prefs
        node.putBoolean(HIDE_PYDEVD_THREADS, DEFAULT_HIDE_PYDEVD_THREADS);
        node.putBoolean(SKIP_CAUGHT_EXCEPTIONS_IN_SAME_FUNCTION, DEFAULT_SKIP_CAUGHT_EXCEPTIONS_IN_SAME_FUNCTION);
        node.putBoolean(IGNORE_EXCEPTIONS_THROWN_IN_LINES_WITH_IGNORE_EXCEPTION,
                DEFAULT_IGNORE_EXCEPTIONS_THROWN_IN_LINES_WITH_IGNORE_EXCEPTION);

        //Prefs on console prompt on debug
        node.putBoolean(SHOW_CONSOLE_PROMPT_ON_DEBUG, true);
        node.putInt(RELATIVE_CONSOLE_HEIGHT, 30);
        node.putInt(CONSOLE_PROMPT_OUTPUT_MODE, MODE_ASYNC_SEPARATE_CONSOLE);

        //Note: the preferences for the debug which appear in the preferences page are actually in
        //the PydevEditorPrefs (as we use the pydev preferences store there).

        // Delegate to the variables preferences
        PyVariablesPreferences.initializeDefaultPreferences();
    }

}
