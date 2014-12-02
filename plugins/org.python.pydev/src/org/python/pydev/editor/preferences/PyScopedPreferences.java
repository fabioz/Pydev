package org.python.pydev.editor.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.preferences.ScopedPreferences;

public class PyScopedPreferences {

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return ScopedPreferences.get(PydevPlugin.DEFAULT_PYDEV_SCOPE).getBoolean(PydevPrefs.getPreferences(),
                setting, projectAdaptable);
    }

    public static String getString(String setting, IAdaptable projectAdaptable) {
        return ScopedPreferences.get(PydevPlugin.DEFAULT_PYDEV_SCOPE).getString(PydevPrefs.getPreferences(),
                setting, projectAdaptable);
    }

    public static int getInt(String setting, IAdaptable projectAdaptable, int minVal) {
        int ret = ScopedPreferences.get(PydevPlugin.DEFAULT_PYDEV_SCOPE).getInt(PydevPrefs.getPreferences(),
                setting, projectAdaptable);
        if (ret < minVal) {
            return minVal;
        }
        return ret;
    }

    public static String getString(String setting, IAdaptable projectAdaptable, String defaultReturn) {
        String ret = getString(setting, projectAdaptable);
        if (ret.isEmpty()) {
            return defaultReturn;
        }
        return ret;
    }

}
