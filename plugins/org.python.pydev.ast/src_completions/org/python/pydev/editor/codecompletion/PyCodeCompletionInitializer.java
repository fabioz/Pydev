/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 20/08/2005
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.logging.PyLoggingPreferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyCodeCompletionInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);

        //use?
        node.putBoolean(PyCodeCompletionPreferences.USE_CODECOMPLETION,
                PyCodeCompletionPreferences.DEFAULT_USE_CODECOMPLETION);
        node.putBoolean(PyCodeCompletionPreferences.USE_CODE_COMPLETION_ON_DEBUG_CONSOLES,
                PyCodeCompletionPreferences.DEFAULT_USE_CODE_COMPLETION_ON_DEBUG_CONSOLES);
        node.putBoolean(PyCodeCompletionPreferences.MATCH_BY_SUBSTRING_IN_CODE_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_MATCH_BY_SUBSTRING_IN_CODE_COMPLETION);

        //Request
        node.putBoolean(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_DOT,
                PyCodeCompletionPreferences.DEFAULT_AUTOCOMPLETE_ON_DOT);
        node.putBoolean(PyCodeCompletionPreferences.USE_AUTOCOMPLETE,
                PyCodeCompletionPreferences.DEFAULT_USE_AUTOCOMPLETE);
        node.putBoolean(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_PAR,
                PyCodeCompletionPreferences.DEFAULT_AUTOCOMPLETE_ON_PAR);
        node.putBoolean(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_ALL_ASCII_CHARS,
                PyCodeCompletionPreferences.DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
        node.putInt(PyCodeCompletionPreferences.MAX_MILLIS_FOR_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_MAX_MILLIS_FOR_COMPLETION);

        //When to apply
        node.putBoolean(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_DOT,
                PyCodeCompletionPreferences.DEFAULT_APPLY_COMPLETION_ON_DOT);
        node.putBoolean(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_LPAREN,
                PyCodeCompletionPreferences.DEFAULT_APPLY_COMPLETION_ON_LPAREN);
        node.putBoolean(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_RPAREN,
                PyCodeCompletionPreferences.DEFAULT_APPLY_COMPLETION_ON_RPAREN);

        //others
        node.putInt(PyCodeCompletionPreferences.ATTEMPTS_CODECOMPLETION,
                PyCodeCompletionPreferences.DEFAULT_ATTEMPTS_CODECOMPLETION);
        node.putInt(PyCodeCompletionPreferences.AUTOCOMPLETE_DELAY,
                PyCodeCompletionPreferences.DEFAULT_AUTOCOMPLETE_DELAY);
        node.putInt(PyCodeCompletionPreferences.ARGUMENTS_DEEP_ANALYSIS_N_CHARS,
                PyCodeCompletionPreferences.DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
        node.putBoolean(PyCodeCompletionPreferences.PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD,
                PyCodeCompletionPreferences.DEFAULT_PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD);

        //Debug
        node.putBoolean(PyLoggingPreferences.DEBUG_CODE_COMPLETION,
                PyLoggingPreferences.DEFAULT_DEBUG_CODE_COMPLETION);
        node.putBoolean(PyLoggingPreferences.DEBUG_ANALYSIS_REQUESTS,
                PyLoggingPreferences.DEFAULT_DEBUG_ANALYSIS_REQUESTS);
        node.putBoolean(PyLoggingPreferences.DEBUG_INTERPRETER_AUTO_UPDATE,
                PyLoggingPreferences.DEFAULT_DEBUG_INTERPRETER_AUTO_UPDATE);

        // Code completion context insensitive
        node.putBoolean(PyCodeCompletionPreferences.USE_KEYWORDS_CODE_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_USE_KEYWORDS_CODE_COMPLETION);
        node.putBoolean(PyCodeCompletionPreferences.ADD_SPACE_WHEN_NEEDED,
                PyCodeCompletionPreferences.DEFAULT_ADD_SPACES_WHEN_NEEDED);
        node.putBoolean(PyCodeCompletionPreferences.ADD_SPACE_AND_COLON_WHEN_NEEDED,
                PyCodeCompletionPreferences.DEFAULT_ADD_SPACES_AND_COLON_WHEN_NEEDED);
        node.putBoolean(PyCodeCompletionPreferences.FORCE_PY3K_PRINT_ON_PY2,
                PyCodeCompletionPreferences.DEFAULT_FORCE_PY3K_PRINT_ON_PY2);
        node.put(PyCodeCompletionPreferences.KEYWORDS_CODE_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_KEYWORDS_CODE_COMPLETION);
        node.putInt(PyCodeCompletionPreferences.CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION);
        node.putInt(PyCodeCompletionPreferences.CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION,
                PyCodeCompletionPreferences.DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION);

    }

}
