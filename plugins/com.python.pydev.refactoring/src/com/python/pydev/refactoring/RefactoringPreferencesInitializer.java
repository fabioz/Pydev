/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;

public class RefactoringPreferencesInitializer extends AbstractPreferenceInitializer {
    public static final String DEFAULT_SCOPE = "com.python.pydev.refactoring";

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(DEFAULT_SCOPE);
        node.putBoolean(MarkOccurrencesPreferencesPage.USE_MARK_OCCURRENCES,
                MarkOccurrencesPreferencesPage.DEFAULT_USE_MARK_OCCURRENCES);
        node.putBoolean(MarkOccurrencesPreferencesPage.USE_MARK_OCCURRENCES_IN_STRINGS,
                MarkOccurrencesPreferencesPage.DEFAULT_USE_MARK_OCCURRENCES_IN_STRINGS);
    }
}
