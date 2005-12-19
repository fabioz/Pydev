package com.python.pydev;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

public class PydevExtensionInitializer extends AbstractPreferenceInitializer{
	public static final String USER_NAME_VALIDATE_EXTENSION = "USER_NAME_VALIDATE_EXTENSION";
	public static final String LICENSE_NUMBER_VALIDATE_EXTENSION = "LICENSE_NUMBER_VALIDATE_EXTENSION";
	
	public static final String DEFAULT_SCOPE = "com.python.pydev";

	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
		
		node.put(USER_NAME_VALIDATE_EXTENSION, "Username");
		node.put(LICENSE_NUMBER_VALIDATE_EXTENSION, "Password");
	}
}
