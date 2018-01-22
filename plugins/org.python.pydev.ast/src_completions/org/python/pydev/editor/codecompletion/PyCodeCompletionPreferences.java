package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyCodeCompletionPreferences {

    public static final String USE_CODECOMPLETION = "USE_CODECOMPLETION";
    public static final boolean DEFAULT_USE_CODECOMPLETION = true;

    public static final String ATTEMPTS_CODECOMPLETION = "ATTEMPTS_CODECOMPLETION";
    public static final int DEFAULT_ATTEMPTS_CODECOMPLETION = 5;

    public static final String AUTOCOMPLETE_ON_DOT = "AUTOCOMPLETE_ON_DOT";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_DOT = true;

    public static final String MAX_MILLIS_FOR_COMPLETION = "MAX_MILLIS_FOR_COMPLETION";
    public static final int DEFAULT_MAX_MILLIS_FOR_COMPLETION = 5 * 1000; //Default is 5 seconds

    public static final String AUTOCOMPLETE_ON_ALL_ASCII_CHARS = "AUTOCOMPLETE_ON_ALL_ASCII_CHARS";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS = true;

    public static final String USE_AUTOCOMPLETE = "USE_AUTOCOMPLETE";
    public static final boolean DEFAULT_USE_AUTOCOMPLETE = true;

    public static final String AUTOCOMPLETE_DELAY = "AUTOCOMPLETE_DELAY";
    public static final int DEFAULT_AUTOCOMPLETE_DELAY = 0;

    public static final String AUTOCOMPLETE_ON_PAR = "AUTOCOMPLETE_ON_PAR";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_PAR = false;

    public static final String APPLY_COMPLETION_ON_DOT = "APPLY_COMPLETION_ON_DOT";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_DOT = false;

    public static final String APPLY_COMPLETION_ON_LPAREN = "APPLY_COMPLETION_ON_LPAREN";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_LPAREN = false;

    public static final String APPLY_COMPLETION_ON_RPAREN = "APPLY_COMPLETION_ON_RPAREN";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_RPAREN = false;

    public static final String ARGUMENTS_DEEP_ANALYSIS_N_CHARS = "DEEP_ANALYSIS_N_CHARS";
    public static final int DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS = 1;

    public static final String USE_CODE_COMPLETION_ON_DEBUG_CONSOLES = "USE_CODE_COMPLETION_ON_DEBUG_CONSOLES";
    public static final boolean DEFAULT_USE_CODE_COMPLETION_ON_DEBUG_CONSOLES = true;

    public static final String MATCH_BY_SUBSTRING_IN_CODE_COMPLETION = "MATCH_BY_SUBSTRING_IN_CODE_COMPLETION";
    public static final boolean DEFAULT_MATCH_BY_SUBSTRING_IN_CODE_COMPLETION = true;

    public static final String PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD = "PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD";
    public static final boolean DEFAULT_PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD = true;

    public static ICallback<IPreferenceStore, Object> getPreferencesForTests;

    public static IPreferenceStore getPreferences() {
        if (SharedCorePlugin.inTestMode()) {
            //always create a new one for tests.
            return getPreferencesForTests.call(null);
        }
        return PydevPrefs.getPreferenceStore();
    }

    public static boolean getPutLocalImportsOnTopOfMethod() {
        return getPreferences().getBoolean(PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD);
    }

    public static boolean useCodeCompletion() {
        return getPreferences().getBoolean(USE_CODECOMPLETION);
    }

    public static boolean useCodeCompletionOnDebug() {
        return getPreferences().getBoolean(USE_CODE_COMPLETION_ON_DEBUG_CONSOLES);
    }

    public static int getNumberOfConnectionAttempts() {
        if (SharedCorePlugin.inTestMode()) {
            return 20;
        }

        IPreferenceStore preferences = getPreferences();
        int ret = preferences.getInt(ATTEMPTS_CODECOMPLETION);
        if (ret < 2) {
            ret = 2; // at least 2 attempts!
        }
        return ret;
    }

    public static int getMaximumNumberOfMillisToCompleteCodeCompletionRequest() {
        int val = getPreferences().getInt(MAX_MILLIS_FOR_COMPLETION);
        if (val <= 200) {
            //Never less than 200 millis
            val = 200;
        }
        if (val >= 120 * 1000) {
            //Never more than 2 minutes
            val = 120 * 1000;
        }
        return val;
    }

    public static boolean isToAutocompleteOnDot() {
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_DOT);
    }

    public static boolean isToAutocompleteOnPar() {
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_PAR);
    }

    public static boolean useAutocomplete() {
        return getPreferences().getBoolean(USE_AUTOCOMPLETE);
    }

    public static boolean useAutocompleteOnAllAsciiChars() {
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
    }

    public static int getAutocompleteDelay() {
        return getPreferences().getInt(AUTOCOMPLETE_DELAY);
    }

    public static int getArgumentsDeepAnalysisNChars() {
        if (SharedCorePlugin.inTestMode()) {
            return 0;
        }
        return getPreferences().getInt(ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
    }

    public static boolean applyCompletionOnDot() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_DOT);
    }

    public static boolean applyCompletionOnLParen() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_LPAREN);
    }

    public static boolean applyCompletionOnRParen() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_RPAREN);
    }

    public static boolean getUseSubstringMatchInCodeCompletion() {
        return getPreferences().getBoolean(MATCH_BY_SUBSTRING_IN_CODE_COMPLETION);
    }

}
