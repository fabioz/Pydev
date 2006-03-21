package org.python.pydev.jython;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;

public class ScriptingExtensionInitializer extends AbstractPreferenceInitializer{
	public static final String DEFAULT_SCOPE = "com.python.pydev.jython";
	
	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
        node.putBoolean(JyScriptingPreferencesPage.SHOW_SCRIPTING_OUTPUT, JyScriptingPreferencesPage.DEFAULT_SHOW_SCRIPTING_OUTPUT);
        node.putBoolean(JyScriptingPreferencesPage.LOG_SCRIPTING_ERRORS, JyScriptingPreferencesPage.DEFAULT_LOG_SCRIPTING_ERRORS);
	}

}
