package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
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
    }

}
