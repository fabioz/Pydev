/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

public class PydevExtensionInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_SCOPE = "com.python.pydev";

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(DEFAULT_SCOPE);

    }
}
