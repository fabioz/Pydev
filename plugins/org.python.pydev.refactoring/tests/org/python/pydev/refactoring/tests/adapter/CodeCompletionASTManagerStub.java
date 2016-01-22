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
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
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

    public Tuple3<IModule, String, IToken> findOnImportedMods(ICompletionState state, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public Tuple3<IModule, String, IToken> findOnImportedMods(IToken[] importedModules, ICompletionState state,
            String currentModuleName, IModule current) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] findTokensOnImportedMods(IToken[] importedModules, ICompletionState state, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public List<IToken> getBuiltinCompletions(ICompletionState state, List<IToken> completions) {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForImport(ImportInfo original, ICompletionRequest request)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForModule(IModule module, ICompletionState state)
            throws CompletionRecursionException {
        return new IToken[] { new SourceToken(new Name("True", Name.Store, true), "True", "", "", "__builtin__"),
                new SourceToken(new Name("False", Name.Store, true), "False", "", "", "__builtin__"), };
    }

    public IToken[] getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForToken(File file, IDocument doc, ICompletionState state)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForToken(IDocument doc, ICompletionState state) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public List<IToken> getGlobalCompletions(IToken[] globalTokens, IToken[] importedModules,
            IToken[] wildImportedModules, ICompletionState state, IModule current) {
        throw new RuntimeException("Not implemented");
    }

    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit, boolean lookingForRelative) {
        throw new RuntimeException("Not implemented");
    }

    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        throw new RuntimeException("Not implemented");
    }

    public IModulesManager getModulesManager() {
        throw new RuntimeException("Not implemented");
    }

    public IPythonNature getNature() {
        throw new RuntimeException("Not implemented");
    }

    public IToken getRepInModule(IModule module, String tokName, IPythonNature nature)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public void rebuildModule(File file, ICallback0<IDocument> doc, IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public ImmutableTuple<IModule, IToken> resolveImport(ICompletionState state, IToken imported, IModule current)
            throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public void setProject(IProject project, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    public void getCompletionsForClassInLocalScope(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion, List<String> lookForClass, HashSet<IToken> hashSet)
                    throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getCompletionsForImport(ImportInfo original, ICompletionRequest request,
            boolean onlyGetDirectModules) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    public boolean getCompletionsForWildImport(ICompletionState state, IModule current, List<IToken> completions,
            IToken wildImport) {
        throw new RuntimeException("Not implemented");
    }

    public Object getLock() {
        return lock;
    }

    public Tuple<IModule, String> findModule(String fromImportStr, String currentModule, ICompletionState state,
            IModule current) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.python.pydev.core.ICodeCompletionASTManager#saveToFile(java.io.File)
     */
    public void saveToFile(File astOutputFile) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IToken[] getCompletionsUnpackingObject(IModule module, ICompletionState copy, ILocalScope scope,
            UnpackInfo unpackPos) throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IToken[] getCompletionsFromTokenInLocalScope(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion, ILocalScope localScope)
                    throws CompletionRecursionException {
        throw new RuntimeException("Not implemented");
    }

}
