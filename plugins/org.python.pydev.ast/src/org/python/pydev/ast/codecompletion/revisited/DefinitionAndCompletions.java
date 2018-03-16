package org.python.pydev.ast.codecompletion.revisited;

import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.TokensList;

public class DefinitionAndCompletions {

    public final TokensList completions;
    public final Definition[] defs;

    public DefinitionAndCompletions(Definition[] defs, TokensList ret) {
        this.defs = defs;
        this.completions = ret;
    }

}
