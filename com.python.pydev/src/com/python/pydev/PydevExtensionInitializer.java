package com.python.pydev;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.interactiveconsole.InteractiveConsolePreferencesPage;

public class PydevExtensionInitializer extends AbstractPreferenceInitializer{
	public static final String USER_NAME = "USER_NAME";
	public static final String USER_EMAIL = "USER_EMAIL";
	public static final String LICENSE = "LICENSE";
	public static final String LIC_TIME = "LIC_TIME";
	public static final String LIC_TYPE = "LIC_TYPE";
	public static final String LIC_DEVS = "LIC_DEVS";
	
	public static final String DEFAULT_SCOPE = "com.python.pydev";

	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
		
		node.put(USER_NAME, "");
		node.put(USER_EMAIL, "");
		node.put(LICENSE, "");
		node.put(LIC_TIME, "");
		node.put(LIC_TYPE, "");
		node.put(LIC_DEVS, "");

        node.putBoolean(InteractiveConsolePreferencesPage.EVAL_ON_NEW_LINE, InteractiveConsolePreferencesPage.DEFAULT_EVAL_ON_NEW_LINE);
        node.putBoolean(InteractiveConsolePreferencesPage.SHOW_CONSOLE_INPUT, InteractiveConsolePreferencesPage.DEFAULT_SHOW_CONSOLE_INPUT);
	}
}
