package org.python.pydev.red_core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.red_core.PydevRedCoreActivator;

public class PydevRedCorePreferencesInitializer extends AbstractPreferenceInitializer {

    public static final String USE_APTANA_THEMES = "PYDEV_USE_APTANA_THEMES";

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevRedCoreActivator.PLUGIN_ID);
        node.putBoolean(USE_APTANA_THEMES, true);
    }

    public static boolean getUseAptanaThemes() {
        return PydevRedCoreActivator.getDefault().getPreferenceStore().getBoolean(USE_APTANA_THEMES);
    }

    public static void setUseAptanaThemes(boolean b) {
        PydevRedCoreActivator.getDefault().getPreferenceStore().setValue(USE_APTANA_THEMES, b);
    }

}
