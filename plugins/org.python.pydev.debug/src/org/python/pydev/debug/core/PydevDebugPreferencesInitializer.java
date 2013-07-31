/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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

        // Delegate to the variables preferences
        PyVariablesPreferences.initializeDefaultPreferences();
    }

}
