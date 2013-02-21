package org.python.pydev.overview_ruler;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.aptana.shared_ui.SharedUiPlugin;

public class MinimapPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(SharedUiPlugin.PLUGIN_ID);

        node.putBoolean(MinimapOverviewRulerPreferencesPage.USE_MINIMAP, false);
        node.putBoolean(MinimapOverviewRulerPreferencesPage.SHOW_SCROLLBAR, true);
        node.putBoolean(MinimapOverviewRulerPreferencesPage.SHOW_MINIMAP_CONTENTS, true);
        node.putInt(MinimapOverviewRulerPreferencesPage.MINIMAP_WIDTH, 100);
    }

}
