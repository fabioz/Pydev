/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;

public class ScriptingExtensionInitializer extends AbstractPreferenceInitializer {
    public static final String DEFAULT_SCOPE = "org.python.pydev.jython";

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(DEFAULT_SCOPE);

        node.putBoolean(JyScriptingPreferencesPage.SHOW_SCRIPTING_OUTPUT,
                JyScriptingPreferencesPage.DEFAULT_SHOW_SCRIPTING_OUTPUT);
        node.putBoolean(JyScriptingPreferencesPage.LOG_SCRIPTING_ERRORS,
                JyScriptingPreferencesPage.DEFAULT_LOG_SCRIPTING_ERRORS);
    }

}
