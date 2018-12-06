/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class MypyPrefInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_QUALIFIER);

        node.put(MypyPreferences.MYPY_FILE_LOCATION, "");
        node.putBoolean(MypyPreferences.USE_MYPY, MypyPreferences.DEFAULT_USE_MYPY);

        node.putBoolean(MypyPreferences.MYPY_USE_CONSOLE, MypyPreferences.DEFAULT_MYPY_USE_CONSOLE);
        node.put(MypyPreferences.MYPY_ARGS, MypyPreferences.DEFAULT_MYPY_ARGS);

    }

}
