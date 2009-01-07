package org.python.pydev.plugin;

import org.eclipse.core.runtime.Preferences;

/**
 * Helper to deal with the pydev preferences.
 * 
 * @author Fabio
 */
public class PydevPrefs {

    /**
     * @return the place where this plugin preferences are stored.
     */
    public static Preferences getPreferences() {
        return PydevPlugin.getDefault().getPluginPreferences();
    }

}
