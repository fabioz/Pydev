package org.python.pydev.ast.codecompletion;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback0;

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

    public static ICallback0<IEclipsePreferences> getPreferencesForTests;

    public static IEclipsePreferences getPreferences() {
        if (SharedCorePlugin.inTestMode()) {
            //always create a new one for tests.
            return getPreferencesForTests.call();
        }
        return PydevPrefs.getEclipsePreferences();
    }

    public static boolean getPutLocalImportsOnTopOfMethod() {
        return getPreferences().getBoolean(PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD,
                DEFAULT_PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD);
    }

    public static boolean useCodeCompletion() {
        return getPreferences().getBoolean(USE_CODECOMPLETION, DEFAULT_USE_CODECOMPLETION);
    }

    public static boolean useCodeCompletionOnDebug() {
        return getPreferences().getBoolean(USE_CODE_COMPLETION_ON_DEBUG_CONSOLES,
                DEFAULT_USE_CODE_COMPLETION_ON_DEBUG_CONSOLES);
    }

    public static int getNumberOfConnectionAttempts() {
        if (SharedCorePlugin.inTestMode()) {
            return 20;
        }

        int ret = getPreferences().getInt(ATTEMPTS_CODECOMPLETION, DEFAULT_ATTEMPTS_CODECOMPLETION);
        if (ret < 2) {
            ret = 2; // at least 2 attempts!
        }
        return ret;
    }

    public static int getMaximumNumberOfMillisToCompleteCodeCompletionRequest() {
        int val = getPreferences().getInt(MAX_MILLIS_FOR_COMPLETION, DEFAULT_MAX_MILLIS_FOR_COMPLETION);
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
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_DOT, DEFAULT_AUTOCOMPLETE_ON_DOT);
    }

    public static boolean isToAutocompleteOnPar() {
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_PAR, DEFAULT_AUTOCOMPLETE_ON_PAR);
    }

    public static boolean useAutocomplete() {
        return getPreferences().getBoolean(USE_AUTOCOMPLETE, DEFAULT_USE_AUTOCOMPLETE);
    }

    public static boolean useAutocompleteOnAllAsciiChars() {
        return getPreferences().getBoolean(AUTOCOMPLETE_ON_ALL_ASCII_CHARS, DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
    }

    public static int getAutocompleteDelay() {
        return getPreferences().getInt(AUTOCOMPLETE_DELAY, DEFAULT_AUTOCOMPLETE_DELAY);
    }

    public static int getArgumentsDeepAnalysisNChars() {
        if (SharedCorePlugin.inTestMode()) {
            return 0;
        }
        return getPreferences().getInt(ARGUMENTS_DEEP_ANALYSIS_N_CHARS, DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
    }

    public static boolean applyCompletionOnDot() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_DOT, DEFAULT_APPLY_COMPLETION_ON_DOT);
    }

    public static boolean applyCompletionOnLParen() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_LPAREN, DEFAULT_APPLY_COMPLETION_ON_LPAREN);
    }

    public static boolean applyCompletionOnRParen() {
        return getPreferences().getBoolean(APPLY_COMPLETION_ON_RPAREN, DEFAULT_APPLY_COMPLETION_ON_RPAREN);
    }

    public static boolean getUseSubstringMatchInCodeCompletion() {
        return getPreferences().getBoolean(MATCH_BY_SUBSTRING_IN_CODE_COMPLETION,
                DEFAULT_MATCH_BY_SUBSTRING_IN_CODE_COMPLETION);
    }

    public static final String USE_KEYWORDS_CODE_COMPLETION = "USE_KEYWORDS_CODE_COMPLETION";
    public static final boolean DEFAULT_USE_KEYWORDS_CODE_COMPLETION = true;

    public static final String ADD_SPACE_WHEN_NEEDED = "ADD_SPACE_WHEN_NEEDED";
    public static final boolean DEFAULT_ADD_SPACES_WHEN_NEEDED = false; //Keep current behavior by default

    public static final String ADD_SPACE_AND_COLON_WHEN_NEEDED = "ADD_SPACE_AND_COLON_WHEN_NEEDED";

    public static final boolean DEFAULT_ADD_SPACES_AND_COLON_WHEN_NEEDED = false; //Keep current behavior by default
    public static final String FORCE_PY3K_PRINT_ON_PY2 = "FORCE_PY3K_PRINT_ON_PY2";
    public static final boolean DEFAULT_FORCE_PY3K_PRINT_ON_PY2 = false;

    public static final String KEYWORDS_CODE_COMPLETION = "KEYWORDS_CODE_COMPLETION";
    public static final String DEFAULT_KEYWORDS_CODE_COMPLETION = defaultKeywordsAsString();

    public static final String CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = 2;

    public static final String CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = 2;

    public static int getIntFromPrefs(String prefName, int defaultVal) {
        if (SharedCorePlugin.inTestMode()) {
            return 1;
        }
        return PydevPrefs.getEclipsePreferences().getInt(prefName, defaultVal);
    }

    public static int getCharsForContextInsensitiveModulesCompletion() {
        String prefName = CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION;
        return getIntFromPrefs(prefName, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION);
    }

    public static int getCharsForContextInsensitiveGlobalTokensCompletion() {
        String prefName = CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION;
        return getIntFromPrefs(prefName, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION);
    }

    public static boolean useKeywordsCodeCompletion() {
        return PydevPrefs.getEclipsePreferences()
                .getBoolean(USE_KEYWORDS_CODE_COMPLETION, DEFAULT_USE_KEYWORDS_CODE_COMPLETION);
    }

    public static boolean addSpaceWhenNeeded() {
        return PydevPrefs.getEclipsePreferences()
                .getBoolean(ADD_SPACE_WHEN_NEEDED, DEFAULT_ADD_SPACES_WHEN_NEEDED);
    }

    public static boolean addSpaceAndColonWhenNeeded() {
        return PydevPrefs.getEclipsePreferences()
                .getBoolean(ADD_SPACE_AND_COLON_WHEN_NEEDED, DEFAULT_ADD_SPACES_AND_COLON_WHEN_NEEDED);
    }

    public static boolean forcePy3kPrintOnPy2() {
        return PydevPrefs.getEclipsePreferences()
                .getBoolean(FORCE_PY3K_PRINT_ON_PY2, DEFAULT_FORCE_PY3K_PRINT_ON_PY2);
    }

    public static String[] getKeywords() {
        String keywords = PydevPrefs.getEclipsePreferences()
                .get(KEYWORDS_CODE_COMPLETION, DEFAULT_KEYWORDS_CODE_COMPLETION);
        return stringAsWords(keywords);
    }

    /**
     * @param keywords keywords to be gotten as string
     * @return a string with all the passed words separated by '\n'
     */
    public static String wordsAsString(String[] keywords) {
        StringBuffer buf = new StringBuffer();
        for (String string : keywords) {
            buf.append(string);
            buf.append("\n");
        }
        return buf.toString();
    }

    public static String defaultKeywordsAsString() {
        String[] KEYWORDS = new String[] { "and", "assert", "break", "class", "continue", "def", "del",
                //                "elif", -- starting with 'e'
                //                "else:", -- starting with 'e'
                //                "except:",  -- ctrl+1 covers for try..except/ starting with 'e'
                //                "exec", -- starting with 'e'
                "finally:", "for", "from", "global",
                //                "if", --too small
                "import",
                //                "in", --too small
                //                "is", --too small
                "lambda", "not",
                //                "or", --too small
                "pass", "print", "raise", "return",
                //                "try:", -- ctrl+1 covers for try..except
                "while", "with", "yield",

                //the ones below were not in the initial list
                "self", "__init__",
                //                "as", --too small
                "False", "None", "object", "True" };
        return wordsAsString(KEYWORDS);
    }

    //very simple cache (this might be requested a lot).
    public static String cache;
    public static String[] cacheRet;

    public static String[] stringAsWords(String keywords) {
        if (cache != null && cache.equals(keywords)) {
            return cacheRet;
        }
        StringTokenizer tokenizer = new StringTokenizer(keywords);
        ArrayList<String> strs = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            strs.add(tokenizer.nextToken());
        }
        cache = keywords;
        cacheRet = strs.toArray(new String[0]);
        return cacheRet;
    }

}
