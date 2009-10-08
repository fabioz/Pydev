package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.core.IGrammarVersionProvider;

public class AdapterPrefs {

    public final String endLineDelim;
    public final IGrammarVersionProvider versionProvider;
    
    public AdapterPrefs(String endLineDelim, IGrammarVersionProvider versionProvider) {
        this.endLineDelim = endLineDelim;
        this.versionProvider = versionProvider;
    }
}
