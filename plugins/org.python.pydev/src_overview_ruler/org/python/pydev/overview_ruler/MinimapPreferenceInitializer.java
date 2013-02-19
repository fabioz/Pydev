package org.python.pydev.overview_ruler;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;

public class MinimapPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);

        node.putBoolean(MinimapOverviewRulerPreferencesPage.USE_MINIMAP, false);
        node.putBoolean(MinimapOverviewRulerPreferencesPage.SHOW_SCROLLBAR, true);
        node.putBoolean(MinimapOverviewRulerPreferencesPage.SHOW_MINIMAP_CONTENTS, true);
        node.putInt(MinimapOverviewRulerPreferencesPage.MINIMAP_WIDTH, 120);
    }

}
