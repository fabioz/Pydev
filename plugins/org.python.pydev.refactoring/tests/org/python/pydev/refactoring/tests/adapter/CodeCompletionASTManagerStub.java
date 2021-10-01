/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 */

package org.python.pydev.refactoring.tests.adapter;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.structure.ImmutableTuple;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

public class CodeCompletionASTManagerStub implements ICodeCompletionASTManager {

    private Object lock = new Object();

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor,
            String defaultSelectedInterpreter) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Tuple3<IModule, String, IToken> findOnImportedMods(ICompletionState state, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Tuple3<IModule, String, IToken> findOnImportedMods(TokensList importedModules, ICompletionState state,
            String currentModuleName, IModule current) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList findTokensOnImportedMods(TokensList importedModules, ICompletionState state, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getBuiltinCompletions(ICompletionState state, TokensList completions) {
        throw new RuntimeException("Not implemented");
    }

    public TokensList getCompletionsForImport(ImportInfo original, ICompletionRequest request)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsForModule(IModule module, ICompletionState state)
            throws CompletionRecursionException {
        return new TokensList(
                new IToken[] { new SourceToken(new Name("True", Name.Store, true), "True", "", "", "__builtin__", null),
                        new SourceToken(new Name("False", Name.Store, true), "False", "", "", "__builtin__", null), });
    }

    @Override
    public TokensList getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public TokensList getCompletionsForToken(File file, IDocument doc, ICompletionState state)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsForToken(IDocument doc, ICompletionState state)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getGlobalCompletions(TokensList globalTokens, TokensList importedModules,
            TokensList wildImportedModules, ICompletionState state, IModule current) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit, boolean lookingForRelative,
            IModuleRequestState moduleRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit,
            IModuleRequestState moduleRequest) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IModulesManager getModulesManager() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPythonNature getNature() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IToken getRepInModule(IModule module, String tokName, IPythonNature nature)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void rebuildModule(File file, ICallback0<IDocument> doc, IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ImmutableTuple<IModule, IToken> resolveImport(ICompletionState state, IToken imported, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public void setProject(IProject project, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsForClassInLocalScope(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion, List<ITypeInfo> lookForClass)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsForImport(ImportInfo original, ICompletionRequest request,
            boolean onlyGetDirectModules) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean getCompletionsForWildImport(ICompletionState state, IModule current, TokensList completions,
            IToken wildImport) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionFromFuncDefReturn(ICompletionState state, IModule s, IDefinition definition,
            boolean considerYieldTheReturnType) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getLock() {
        return lock;
    }

    @Override
    public Tuple<IModule, String> findModule(String fromImportStr, String currentModule, ICompletionState state,
            IModule current) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.python.pydev.core.ICodeCompletionASTManager#saveToFile(java.io.File)
     */
    @Override
    public void saveToFile(File astOutputFile) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsUnpackingObject(IModule module, ICompletionState copy, ILocalScope scope,
            UnpackInfo unpackPos) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TokensList getCompletionsFromTokenInLocalScope(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion, ILocalScope localScope)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IModule getPyiStubModule(IModule module, ICompletionState completionState) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isNodeTypingUnionSubscript(IModule module, Object node) {
        throw new RuntimeException("Not implemented");
    }

}
