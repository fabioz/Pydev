package com.python.pydev.analysis;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.preferences.IScopedPreferences;

public class PyAnalysisScopedPreferences {

    public static final String ANALYSIS_SCOPE = SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE;

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return get().getBoolean(PydevPrefs.getAnalysisEclipsePreferences(),
                PydevPrefs.getDefaultAnalysisEclipsePreferences(), setting, projectAdaptable);
    }

    public static String getString(String setting, IAdaptable projectAdaptable) {
        return get().getString(PydevPrefs.getAnalysisEclipsePreferences(),
                PydevPrefs.getDefaultAnalysisEclipsePreferences(), setting, projectAdaptable);
    }

    public static int getInt(String setting, IAdaptable projectAdaptable, int minVal) {
        int ret = get().getInt(PydevPrefs.getAnalysisEclipsePreferences(),
                PydevPrefs.getDefaultAnalysisEclipsePreferences(), setting, projectAdaptable);
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
        // Note that we save/load all preferences to/from the default org.python.pydev scope now!
        return PyScopedPreferences.get();
    }

}
