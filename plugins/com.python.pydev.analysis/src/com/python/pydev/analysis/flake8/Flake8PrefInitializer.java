/**
 * Copyright (c) 2021 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.flake8;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class Flake8PrefInitializer {

    public static void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);

        node.put(Flake8Preferences.FLAKE8_FILE_LOCATION, "");
        node.putBoolean(Flake8Preferences.USE_FLAKE8, Flake8Preferences.DEFAULT_USE_FLAKE8);
        node.putBoolean(Flake8Preferences.FLAKE8_USE_CONSOLE, Flake8Preferences.DEFAULT_FLAKE8_USE_CONSOLE);
        node.put(Flake8Preferences.FLAKE8_ARGS, Flake8Preferences.DEFAULT_FLAKE8_ARGS);
        node.put(Flake8Preferences.FLAKE8_CODES_CONFIG, Flake8Preferences.DEFAULT_FLAKE8_CODES_CONFIG);
    }

}
