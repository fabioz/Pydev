package org.python.pydev.plugin.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.preferences.ScopedPreferences;

public class PyScopedPreferences {

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return get().getBoolean(PydevPrefs.getPreferences(), setting, projectAdaptable);
    }

    public static String getString(String setting, IAdaptable projectAdaptable) {
        return get().getString(PydevPrefs.getPreferences(), setting, projectAdaptable);
    }

    public static int getInt(String setting, IAdaptable projectAdaptable, int minVal) {
        int ret = get().getInt(PydevPrefs.getPreferences(), setting, projectAdaptable);
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

    public static IScopedPreferences get() {
        return ScopedPreferences.get(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);
    }

}
