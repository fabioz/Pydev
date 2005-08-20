/*
 * Created on 20/08/2005
 */
package org.python.pydev.builder.pylint;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;


public class PyLintPrefInitializer extends AbstractPreferenceInitializer{

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);
        
        node.put(PyLintPrefPage.PYLINT_FILE_LOCATION, "");
        node.putBoolean(PyLintPrefPage.USE_PYLINT, PyLintPrefPage.DEFAULT_USE_PYLINT);
        node.putBoolean(PyLintPrefPage.USE_ERRORS, PyLintPrefPage.DEFAULT_USE_ERRORS);
        node.putBoolean(PyLintPrefPage.USE_WARNINGS, PyLintPrefPage.DEFAULT_USE_WARNINGS);
        node.putBoolean(PyLintPrefPage.USE_FATAL, PyLintPrefPage.DEFAULT_USE_FATAL);
        node.putBoolean(PyLintPrefPage.USE_CODING_STANDARD, PyLintPrefPage.DEFAULT_USE_CODING_STANDARD);
        node.putBoolean(PyLintPrefPage.USE_REFACTOR, PyLintPrefPage.DEFAULT_USE_REFACTOR);
        node.put(PyLintPrefPage.PYLINT_ARGS, PyLintPrefPage.DEFAULT_PYLINT_ARGS);
        node.putInt(PyLintPrefPage.MAX_PYLINT_DELTA, PyLintPrefPage.DEFAULT_MAX_PYLINT_DELTA);

    }

}
