/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IToken;
import org.python.pydev.shared_core.string.FastStringBuffer;

public final class GenAndTok {

    /**
     * This is the token that is from the current module that created the token (if on some wild import)
     *
     * May be equal to tok
     */
    public final IToken generator;

    /**
     * This is the token that has been added to the namespace (may have been created on the current module or not).
     */
    public final IToken tok;

    /**
     * These are the tokens that refer this generator
     */
    public final List<IToken> references = new ArrayList<IToken>();

    /**
     * the scope id of the definition
     */
    public final int scopeId;

    /**
     * this is the scope where it was found
     */
    public final ScopeItems scopeFound;

    /**
     * Determines whether it was in a typing.TYPE_CHECKING if when it was found.
     */
    public final boolean inTypeChecking;

    public GenAndTok(IToken generator, IToken tok, int scopeId, ScopeItems scopeFound) {
        this.generator = generator;
        this.tok = tok;
        this.scopeId = scopeId;
        this.scopeFound = scopeFound;
        this.inTypeChecking = scopeFound.isInTypeChecking();
    }

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("GenAndTok [ ");

        buffer.append(generator.getRepresentation());
        buffer.append(" - ");
        buffer.append(tok.getRepresentation());

        buffer.append(" (scopeId:");
        buffer.append(scopeId);
        buffer.append(") ");

        if (references.size() > 0) {
            buffer.append(" (references:");
            for (IToken ref : references) {
                buffer.append(ref.getRepresentation());
                buffer.append(",");
            }
            buffer.deleteLast(); //remove the last comma
            buffer.append(") ");
        }

        buffer.append("]");
        return buffer.toString();
    }

    public List<IToken> getAllTokens() {
        ArrayList<IToken> ret = new ArrayList<IToken>();
        ret.add(generator);
        ret.addAll(references);
        return ret;
    }
}
