/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IToken;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.string.FastStringBuffer;

import com.python.pydev.analysis.visitors.ImportChecker.ImportInfo;

public final class Found {

    private final List<GenAndTok> found = new ArrayList<GenAndTok>();

    /**
     * Identifies if the current token has been used or not
     */
    private boolean used = false;

    /**
     * If this is an import, it may be resolved to some module and some token within that module...
     */
    public ImportInfo importInfo;

    private GenAndTok lastGenAndTok;

    private CallbackWithListeners<Found> onDefined;

    public Found(IToken tok, IToken generator, int scopeId, ScopeItems scopeFound) {
        GenAndTok o = new GenAndTok(generator, tok, scopeId, scopeFound);
        lastGenAndTok = o;
        this.found.add(o);
    }

    /**
     * Registers a callback to be called if it's later defined.
     */
    public void registerCallOnDefined(final ICallbackListener<Found> listener) {
        if (onDefined == null) {
            onDefined = new CallbackWithListeners<Found>();
        }
        onDefined.registerListener(listener);
    }

    /**
     * Called to report how it was found later on (only called if it was initially undefined and was found as
     * being a definition from the actual module later on).
     */
    public void reportDefined(Found laterFound) {
        if (onDefined != null) {
            onDefined.call(laterFound);
        }
    }

    /**
     * @param used The used to set.
     */
    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * @return Returns the used.
     */
    public boolean isUsed() {
        return used;
    }

    public void addGeneratorToFound(IToken generator2, IToken tok2, int scopeId, ScopeItems scopeFound) {
        GenAndTok o = new GenAndTok(generator2, tok2, scopeId, scopeFound);
        lastGenAndTok = o;
        this.found.add(o);
    }

    public void addGeneratorsFromFound(Found found2) {
        if (found2.found.size() > 0) {
            this.found.addAll(found2.found);
            lastGenAndTok = this.found.get(this.found.size() - 1);
        }
    }

    public GenAndTok getSingle() {
        return lastGenAndTok; //always returns the last (this is the one that is binded at the current place in the scope)
    }

    public List<GenAndTok> getAll() {
        return found;
    }

    public boolean isImport() {
        return lastGenAndTok.generator.isImport();
    }

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Found { (used:");
        buffer.append(used);
        buffer.append(") [");

        for (GenAndTok g : found) {
            buffer.appendObject(g);
            buffer.append("  ");
        }
        buffer.append(" ]}");
        return buffer.toString();
    }

    public boolean isWildImport() {
        return lastGenAndTok.generator.isWildImport();
    }

}