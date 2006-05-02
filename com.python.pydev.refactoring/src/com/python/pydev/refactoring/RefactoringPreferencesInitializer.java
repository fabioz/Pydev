package com.python.pydev.refactoring;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;

public class RefactoringPreferencesInitializer extends AbstractPreferenceInitializer{
	public static final String DEFAULT_SCOPE = "com.python.pydev.refactoring";
	
	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
        node.putBoolean(MarkOccurrencesPreferencesPage.USE_MARK_OCCURRENCES, MarkOccurrencesPreferencesPage.DEFAULT_USE_MARK_OCCURRENCES);
	}
}
