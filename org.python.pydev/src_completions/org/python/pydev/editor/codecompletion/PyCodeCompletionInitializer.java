/*
 * Created on 20/08/2005
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;

public class PyCodeCompletionInitializer extends AbstractPreferenceInitializer{

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);
        
    	node.putBoolean(PyCodeCompletionPreferencesPage.USE_CODECOMPLETION, PyCodeCompletionPreferencesPage.DEFAULT_USE_CODECOMPLETION);
    	node.putInt(PyCodeCompletionPreferencesPage.ATTEMPTS_CODECOMPLETION, PyCodeCompletionPreferencesPage.DEFAULT_ATTEMPTS_CODECOMPLETION);
    	node.putBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_DOT, PyCodeCompletionPreferencesPage.DEFAULT_AUTOCOMPLETE_ON_DOT);
    	node.putBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_ALL_ASCII_CHARS, PyCodeCompletionPreferencesPage.DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
    	node.putBoolean(PyCodeCompletionPreferencesPage.USE_AUTOCOMPLETE, PyCodeCompletionPreferencesPage.DEFAULT_USE_AUTOCOMPLETE);
    	node.putInt(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_DELAY, PyCodeCompletionPreferencesPage.DEFAULT_AUTOCOMPLETE_DELAY);
    	node.putBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_PAR, PyCodeCompletionPreferencesPage.DEFAULT_AUTOCOMPLETE_ON_PAR);
    	node.putBoolean(PyCodeCompletionPreferencesPage.DEBUG_CODE_COMPLETION, PyCodeCompletionPreferencesPage.DEFAULT_DEBUG_CODE_COMPLETION);
    	node.putInt(PyCodeCompletionPreferencesPage.ARGUMENTS_DEEP_ANALYSIS_N_CHARS, PyCodeCompletionPreferencesPage.DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
    }

}
