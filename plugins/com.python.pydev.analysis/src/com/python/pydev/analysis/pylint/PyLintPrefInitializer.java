/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 20/08/2005
 */
package com.python.pydev.analysis.pylint;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyLintPrefInitializer {

    public static void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_QUALIFIER);

        node.put(PyLintPreferences.PYLINT_FILE_LOCATION, "");
        node.putBoolean(PyLintPreferences.USE_PYLINT, PyLintPreferences.DEFAULT_USE_PYLINT);

        node.putInt(PyLintPreferences.SEVERITY_ERRORS, PyLintPreferences.DEFAULT_SEVERITY_ERRORS);
        node.putInt(PyLintPreferences.SEVERITY_WARNINGS, PyLintPreferences.DEFAULT_SEVERITY_WARNINGS);
        node.putInt(PyLintPreferences.SEVERITY_FATAL, PyLintPreferences.DEFAULT_SEVERITY_FATAL);
        node.putInt(PyLintPreferences.SEVERITY_CODING_STANDARD, PyLintPreferences.DEFAULT_SEVERITY_CODING_STANDARD);
        node.putInt(PyLintPreferences.SEVERITY_REFACTOR, PyLintPreferences.DEFAULT_SEVERITY_REFACTOR);

        node.putBoolean(PyLintPreferences.USE_CONSOLE, PyLintPreferences.DEFAULT_USE_CONSOLE);
        node.put(PyLintPreferences.PYLINT_ARGS, PyLintPreferences.DEFAULT_PYLINT_ARGS);

    }

}
