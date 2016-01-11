/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 20/08/2005
 */
package org.python.pydev.builder.pylint;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;

public class PyLintPrefInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);

        node.put(PyLintPrefPage.PYLINT_FILE_LOCATION, "");
        node.putBoolean(PyLintPrefPage.USE_PYLINT, PyLintPrefPage.DEFAULT_USE_PYLINT);

        node.putInt(PyLintPrefPage.SEVERITY_ERRORS, PyLintPrefPage.DEFAULT_SEVERITY_ERRORS);
        node.putInt(PyLintPrefPage.SEVERITY_WARNINGS, PyLintPrefPage.DEFAULT_SEVERITY_WARNINGS);
        node.putInt(PyLintPrefPage.SEVERITY_FATAL, PyLintPrefPage.DEFAULT_SEVERITY_FATAL);
        node.putInt(PyLintPrefPage.SEVERITY_CODING_STANDARD, PyLintPrefPage.DEFAULT_SEVERITY_CODING_STANDARD);
        node.putInt(PyLintPrefPage.SEVERITY_REFACTOR, PyLintPrefPage.DEFAULT_SEVERITY_REFACTOR);

        node.putBoolean(PyLintPrefPage.USE_CONSOLE, PyLintPrefPage.DEFAULT_USE_CONSOLE);
        node.put(PyLintPrefPage.PYLINT_ARGS, PyLintPrefPage.DEFAULT_PYLINT_ARGS);
        node.putInt(PyLintPrefPage.MAX_PYLINT_DELTA, PyLintPrefPage.DEFAULT_MAX_PYLINT_DELTA);

    }

}
