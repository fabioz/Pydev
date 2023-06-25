/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ruff;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class RuffPrefInitializer {

    public static void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);

        node.put(RuffPreferences.RUFF_FILE_LOCATION, "");
        node.putBoolean(RuffPreferences.USE_RUFF, RuffPreferences.DEFAULT_USE_RUFF);

        node.putBoolean(RuffPreferences.RUFF_USE_CONSOLE, RuffPreferences.DEFAULT_RUFF_USE_CONSOLE);
        node.put(RuffPreferences.RUFF_ARGS, RuffPreferences.DEFAULT_RUFF_ARGS);
    }

}
