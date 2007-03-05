package org.python.pydev.refactoring.ast.adapters;

import java.util.List;

import org.python.pydev.core.IToken;


public class ClassDefAdapterFromTokens extends ClassDefAdapter{

    private List<IToken> tokens;

    public ClassDefAdapterFromTokens(List<IToken> tokens) {
        super(null, null, null);
        this.tokens = tokens;
    }

}
