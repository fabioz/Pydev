package com.python.pydev.analysis;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.preferences.ScopedPreferences;

public class PyAnalysisScopedPreferences {

    public static final String ANALYSIS_SCOPE = "org.python.pydev.analysis";

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return get().getBoolean(AnalysisPlugin.getDefault().getPreferenceStore(), setting, projectAdaptable);
    }

    public static String getString(String setting, IAdaptable projectAdaptable) {
        return get().getString(AnalysisPlugin.getDefault().getPreferenceStore(), setting, projectAdaptable);
    }

    public static int getInt(String setting, IAdaptable projectAdaptable, int minVal) {
        int ret = get().getInt(AnalysisPlugin.getDefault().getPreferenceStore(), setting, projectAdaptable);
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
        // Note: our bundle is com.python.pydev.analysis, but for the user it can be presented as 
        // org.python.pydev.analysis as it's like that only because of historical reasons.
        return ScopedPreferences.get(ANALYSIS_SCOPE);
    }

}
