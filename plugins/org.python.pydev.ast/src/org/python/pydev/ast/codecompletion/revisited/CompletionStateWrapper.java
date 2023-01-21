/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.NoExceptionCloseable;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.FastStringBuffer;

public final class CompletionStateWrapper implements ICompletionState {

    private ICompletionState wrapped;

    public CompletionStateWrapper(CompletionState state) {
        this.wrapped = state;
        this.activationToken = state.getActivationToken();
        this.localImportsGotten = state.getLocalImportsGotten();
        this.qualifier = state.getQualifier();
    }

    //things that are not delegated ------------------------------------------------------------------------------------
    private String activationToken;
    private String qualifier;
    private int col = -1;
    private int line = -1;
    private boolean localImportsGotten;

    @Override
    public void setCancelMonitor(IProgressMonitor cancelMonitor) {
        this.wrapped.setCancelMonitor(cancelMonitor);
    }

    @Override
    public String getActivationToken() {
        return activationToken;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
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

    @Override
    public boolean getAcceptTypeshed() {
        return wrapped.getAcceptTypeshed();
    }

    @Override
    public void setAcceptTypeshed(boolean acceptTypeshed) {
        wrapped.setAcceptTypeshed(acceptTypeshed);
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
    public void checkUnpackMemory(IModule module, String string, int beginLine, int beginColumn)
            throws CompletionRecursionException {
        wrapped.checkUnpackMemory(module, string, beginLine, beginColumn);
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
    public void checkLookForFunctionDefReturn(IModule module, ISimpleNode node) throws CompletionRecursionException {
        wrapped.checkLookForFunctionDefReturn(module, node);
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
        return wrapped.getCopyWithActTok(this.activationToken);
    }

    @Override
    public ICompletionState getCopyForResolveImportWithActTok(String representation) {
        return wrapped.getCopyForResolveImportWithActTok(representation);
    }

    @Override
    public ICompletionState getCopyWithActTok(String value) {
        return wrapped.getCopyWithActTok(value);
    }

    /**
     * @param line starting at 0
     * @param col starting at 0
     */
    @Override
    public ICompletionState getCopyWithActTok(String value, int line, int col) {
        return wrapped.getCopyWithActTok(value, line, col);
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
    public LookingFor getLookingFor() {
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
    public void setLookingFor(LookingFor b) {
        wrapped.setLookingFor(b);
    }

    @Override
    public void setLookingFor(LookingFor b, boolean force) {
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
    public TokensList getTokenImportedModules() {
        return wrapped.getTokenImportedModules();
    }

    @Override
    public void setTokenImportedModules(TokensList tokenImportedModules) {
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

    @Override
    public ModuleHandleOrNotGotten getPyiStubModule(IModule module) {
        return this.wrapped.getPyiStubModule(module);
    }

    @Override
    public void setPyIStubModule(IModule module, IModule pyIModule) {
        this.wrapped.setPyIStubModule(pyIModule, pyIModule);
    }

    @Override
    public NoExceptionCloseable pushLookingFor(LookingFor lookingForInstancedVariable) {
        return this.wrapped.pushLookingFor(lookingForInstancedVariable);
    }

    @Override
    public boolean isResolvingBuiltins() {
        return this.wrapped.isResolvingBuiltins();
    }

    @Override
    public void pushResolvingBuiltins() {
        this.wrapped.pushResolvingBuiltins();
    }

    @Override
    public void popResolvingBuiltins() {
        this.wrapped.popResolvingBuiltins();
    }

    @Override
    public void pushSkipObjectBaseCompletions() {
        this.wrapped.pushSkipObjectBaseCompletions();
    }

    @Override
    public void popSkipObjectBaseCompletions() {
        this.wrapped.popSkipObjectBaseCompletions();
    }

    @Override
    public boolean getSkipObjectBaseCompletions() {
        return this.wrapped.getSkipObjectBaseCompletions();

    }

    @Override
    public boolean pushGettingCompletionsFromTokenInLocalScope(IModule module, String activationToken,
            ILocalScope localScope) {
        return this.wrapped.pushGettingCompletionsFromTokenInLocalScope(module, activationToken, localScope);
    }

    @Override
    public void popGettingCompletionsFromTokenInLocalScope(IModule module, String activationToken,
            ILocalScope localScope) {
        this.wrapped.popGettingCompletionsFromTokenInLocalScope(module, activationToken, localScope);
    }

}
