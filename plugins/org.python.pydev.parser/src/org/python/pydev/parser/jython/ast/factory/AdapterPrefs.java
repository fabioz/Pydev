package org.python.pydev.parser.jython.ast.factory;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IGrammarVersionProvider;

public class AdapterPrefs {

    public final String endLineDelim;
    public final IGrammarVersionProvider versionProvider;
    public final IAdaptable projectAdaptable;

    public AdapterPrefs(String endLineDelim, IGrammarVersionProvider versionProvider) {
        this.endLineDelim = endLineDelim;
        this.versionProvider = versionProvider;
        if (versionProvider instanceof IAdaptable) {
            projectAdaptable = (IAdaptable) versionProvider;
        } else {
            projectAdaptable = null;
        }
    }
}
