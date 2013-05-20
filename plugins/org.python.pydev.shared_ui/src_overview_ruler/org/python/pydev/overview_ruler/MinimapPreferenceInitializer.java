/**
 * Copyright (c) 2013 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_ui.SharedUiPlugin;

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
