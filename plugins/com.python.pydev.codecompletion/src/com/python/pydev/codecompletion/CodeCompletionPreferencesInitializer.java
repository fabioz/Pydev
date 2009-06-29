/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.codecompletion.simpleassist.KeywordsSimpleAssist;

public class CodeCompletionPreferencesInitializer extends AbstractPreferenceInitializer{

    public static final String DEFAULT_SCOPE = "com.python.pydev.codecompletion";

    public static final String USE_KEYWORDS_CODE_COMPLETION = "USE_KEYWORDS_CODE_COMPLETION";
    public static final boolean DEFAULT_USE_KEYWORDS_CODE_COMPLETION = true;

    public static final String KEYWORDS_CODE_COMPLETION = "KEYWORDS_CODE_COMPLETION";
    public static final String DEFAULT_KEYWORDS_CODE_COMPLETION = KeywordsSimpleAssist.defaultKeywordsAsString();
    
    public static final String CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = 2;
    
    public static final String CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = 2;
    
    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
        
        node.putBoolean(USE_KEYWORDS_CODE_COMPLETION, DEFAULT_USE_KEYWORDS_CODE_COMPLETION);
        node.put(KEYWORDS_CODE_COMPLETION, DEFAULT_KEYWORDS_CODE_COMPLETION);
        node.putInt(CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION);
        node.putInt(CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION);
    }

}
