/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.codecompletion.simpleassist.KeywordsSimpleAssist;

public class CodeCompletionPreferencesInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_SCOPE = "com.python.pydev.codecompletion";

    public static final String USE_KEYWORDS_CODE_COMPLETION = "USE_KEYWORDS_CODE_COMPLETION";
    public static final boolean DEFAULT_USE_KEYWORDS_CODE_COMPLETION = true;

    public static final String ADD_SPACE_WHEN_NEEDED = "ADD_SPACE_WHEN_NEEDED";
    public static final boolean DEFAULT_ADD_SPACES_WHEN_NEEDED = false; //Keep current behavior by default

    public static final String ADD_SPACE_AND_COLON_WHEN_NEEDED = "ADD_SPACE_AND_COLON_WHEN_NEEDED";
    public static final boolean DEFAULT_ADD_SPACES_AND_COLON_WHEN_NEEDED = false; //Keep current behavior by default

    public static final String FORCE_PY3K_PRINT_ON_PY2 = "FORCE_PY3K_PRINT_ON_PY2";
    public static final boolean DEFAULT_FORCE_PY3K_PRINT_ON_PY2 = false;

    public static final String KEYWORDS_CODE_COMPLETION = "KEYWORDS_CODE_COMPLETION";
    public static final String DEFAULT_KEYWORDS_CODE_COMPLETION = KeywordsSimpleAssist.defaultKeywordsAsString();

    public static final String CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION = 2;

    public static final String CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = "CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION";
    public static final int DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION = 2;

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(DEFAULT_SCOPE);

        node.putBoolean(USE_KEYWORDS_CODE_COMPLETION, DEFAULT_USE_KEYWORDS_CODE_COMPLETION);
        node.putBoolean(ADD_SPACE_WHEN_NEEDED, DEFAULT_ADD_SPACES_WHEN_NEEDED);
        node.putBoolean(ADD_SPACE_AND_COLON_WHEN_NEEDED, DEFAULT_ADD_SPACES_AND_COLON_WHEN_NEEDED);
        node.putBoolean(FORCE_PY3K_PRINT_ON_PY2, DEFAULT_FORCE_PY3K_PRINT_ON_PY2);
        node.put(KEYWORDS_CODE_COMPLETION, DEFAULT_KEYWORDS_CODE_COMPLETION);
        node.putInt(CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION);
        node.putInt(CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION, DEFAULT_CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION);
    }

}
