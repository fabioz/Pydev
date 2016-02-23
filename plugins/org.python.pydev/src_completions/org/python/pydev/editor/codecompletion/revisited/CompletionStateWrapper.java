/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.List;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.string.FastStringBuffer;

public final class CompletionStateWrapper implements ICompletionState {

    private ICompletionState wrapped;

    public CompletionStateWrapper(CompletionState state) {
        this.wrapped = state;
        this.activationToken = state.getActivationToken();
        this.localImportsGotten = state.getLocalImportsGotten();
    }

    //things that are not delegated ------------------------------------------------------------------------------------
    private String activationToken;
    private int col = -1;
    private int line = -1;
    private boolean localImportsGotten;

    @Override
    public String getActivationToken() {
        return activationToken;
    }

    @Override
    public void setActivationToken(String string) {
        activationToken = string;
    }

    @Override
    public String getFullActivationToken() {
        return this.wrapped.getFullActivationToken();
    }

    @Override
    public void setFullActivationToken(String act) {
        this.wrapped.setFullActivationToken(act);
    }

    @Override
    public boolean getLocalImportsGotten() {
        return localImportsGotten;
    }

    @Override
    public void setLocalImportsGotten(boolean b) {
        localImportsGotten = b;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public void setCol(int i) {
        col = i;
    }

    @Override
    public void setLine(int i) {
        line = i;
    }

    //delegated --------------------------------------------------------------------------------------------------------
    @Override
    public void checkDefinitionMemory(IModule module, IDefinition definition) throws CompletionRecursionException {
        wrapped.checkDefinitionMemory(module, definition);
    }

    @Override
    public void checkMaxTimeForCompletion() throws CompletionRecursionException {
        wrapped.checkMaxTimeForCompletion();
    }

    @Override
    public void checkFindLocalDefinedDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.checkFindLocalDefinedDefinitionMemory(mod, tok);
    }

    @Override
    public void checkFindDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.checkFindDefinitionMemory(mod, tok);
    }

    @Override
    public void checkFindMemory(IModule module, String value) throws CompletionRecursionException {
        wrapped.checkFindMemory(module, value);
    }

    @Override
    public void checkFindModuleCompletionsMemory(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.checkFindModuleCompletionsMemory(mod, tok);
    }

    @Override
    public void checkFindResolveImportMemory(IToken tok) throws CompletionRecursionException {
        wrapped.checkFindResolveImportMemory(tok);
    }

    @Override
    public void checkMemory(IModule module, String base) throws CompletionRecursionException {
        wrapped.checkMemory(module, base);
    }

    @Override
    public void checkResolveImportMemory(IModule module, String value) throws CompletionRecursionException {
        wrapped.checkResolveImportMemory(module, value);
    }

    @Override
    public void checkWildImportInMemory(IModule current, IModule mod) throws CompletionRecursionException {
        wrapped.checkWildImportInMemory(current, mod);
    }

    @Override
    public boolean checkFoudSameDefinition(int line, int col, IModule mod) {
        return wrapped.checkFoudSameDefinition(line, col, mod);
    }

    @Override
    public boolean canStillCheckFindSourceFromCompiled(IModule mod, String tok) {
        return wrapped.canStillCheckFindSourceFromCompiled(mod, tok);
    }

    @Override
    public boolean getBuiltinsGotten() {
        return wrapped.getBuiltinsGotten();
    }

    @Override
    public ICompletionState getCopy() {
        return wrapped.getCopy();
    }

    @Override
    public ICompletionState getCopyForResolveImportWithActTok(String representation) {
        return wrapped.getCopyForResolveImportWithActTok(representation);
    }

    @Override
    public ICompletionState getCopyWithActTok(String value) {
        return wrapped.getCopyWithActTok(value);
    }

    @Override
    public boolean getIsInCalltip() {
        return wrapped.getIsInCalltip();
    }

    @Override
    public IPythonNature getNature() {
        return wrapped.getNature();
    }

    @Override
    public String getQualifier() {
        return wrapped.getQualifier();
    }

    @Override
    public int getLookingFor() {
        return wrapped.getLookingFor();
    }

    @Override
    public void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.raiseNFindTokensOnImportedModsCalled(mod, tok);
    }

    @Override
    public void setBuiltinsGotten(boolean b) {
        wrapped.setBuiltinsGotten(b);
    }

    @Override
    public void setIsInCalltip(boolean isInCalltip) {
        wrapped.setIsInCalltip(isInCalltip);
    }

    @Override
    public void setLookingFor(int b) {
        wrapped.setLookingFor(b);
    }

    @Override
    public void setLookingFor(int b, boolean force) {
        wrapped.setLookingFor(b, force);
    }

    @Override
    public void popFindResolveImportMemoryCtx() {
        wrapped.popFindResolveImportMemoryCtx();
    }

    @Override
    public void pushFindResolveImportMemoryCtx() {
        wrapped.pushFindResolveImportMemoryCtx();
    }

    @Override
    public List<IToken> getTokenImportedModules() {
        return wrapped.getTokenImportedModules();
    }

    @Override
    public void setTokenImportedModules(List<IToken> tokenImportedModules) {
        wrapped.setTokenImportedModules(tokenImportedModules);
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("CompletionStateWrapper[ ");
        buf.append(this.activationToken);
        buf.append(" ]");
        return buf.toString();
    }

    @Override
    public void add(Object key, Object n) {
        this.wrapped.add(key, n);
    }

    @Override
    public Object getObj(Object o) {
        return this.wrapped.getObj(o);
    }

    @Override
    public void remove(Object key) {
        this.wrapped.remove(key);
    }

    @Override
    public void clear() {
        this.wrapped.clear();
    }

    @Override
    public void removeStaleEntries() {
        this.wrapped.removeStaleEntries();
    }

    @Override
    public int pushAssign() {
        return this.wrapped.pushAssign();
    }

    @Override
    public void popAssign() {
        this.wrapped.popAssign();
    }

    @Override
    public boolean getAlreadySearchedInAssign(int line, int col, IModule module, String value, String actTok) {
        return this.wrapped.getAlreadySearchedInAssign(line, col, module, value, actTok);
    }

    @Override
    public void pushGetCompletionsUnpackingObject() throws CompletionRecursionException {
        this.wrapped.pushGetCompletionsUnpackingObject();
    }

    @Override
    public void popGetCompletionsUnpackingObject() {
        this.wrapped.popGetCompletionsUnpackingObject();
    }
}
