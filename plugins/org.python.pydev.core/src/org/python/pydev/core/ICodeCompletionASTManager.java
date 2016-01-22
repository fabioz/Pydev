/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.structure.ImmutableTuple;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * @author Fabio Zadrozny
 */
public interface ICodeCompletionASTManager {

    /**
     * This method rebuilds the paths that can be used for the code completion.
     * It doesn't load the modules, only the paths.
     * @param defaultSelectedInterpreter
     *
     * @param pythonpath: string with paths separated by |
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor);

    /**
     * Set the project this ast manager works with.
     *
     * @param project the project related to this ast manager
     * @param restoreDeltas says whether deltas should be restored (if they are not, they should be discarded)
     */
    public abstract void setProject(IProject project, IPythonNature nature, boolean restoreDeltas);

    /**
     * This method provides a way to rebuild a module (new delta).
     *
     * @param file: file that represents a module
     * @param doc
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void rebuildModule(final File file, final ICallback0<IDocument> doc, final IProject project,
            IProgressMonitor monitor, IPythonNature nature);

    /**
     * This method provides a way to remove a module (remove delta).
     *
     * @param file: file that represents a module
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void removeModule(final File file, final IProject project, IProgressMonitor monitor);

    /**
     * @return the modules manager associated with this manager.
     */
    public abstract IModulesManager getModulesManager();

    /**
     * @return the nature associated to this manager
     */
    public abstract IPythonNature getNature();

    public static class ImportInfo {
        public String importsTipperStr;
        public final boolean hasImportSubstring;
        public final boolean hasFromSubstring;

        public ImportInfo(String importsTipperStr, boolean hasImportSubstring, boolean hasFromSubstring) {
            this.importsTipperStr = importsTipperStr;
            this.hasImportSubstring = hasImportSubstring;
            this.hasFromSubstring = hasFromSubstring;
        }
    }

    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparison is not case dependent. Passes all the modules in the cache.
     *
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     * @throws CompletionRecursionException
     * @throws MisconfigurationException
     */
    public abstract IToken[] getCompletionsForImport(ImportInfo original, ICompletionRequest request,
            boolean onlyGetDirectModules) throws CompletionRecursionException, MisconfigurationException;

    /**
     * The completion should work in the following way:
     *
     * First we have to know in which scope we are.
     *
     * If we have no token nor qualifier, get the locals for the file (only from module imports or from inner scope).
     *
     * If we have a part of the qualifier and not activationToken, go for all that match (e.g. all classes, so that we can make the import
     * automatically)
     *
     * If we have the activationToken, try to guess what it is and get its attrs and funcs.
     *
     * @param file
     * @param doc
     * @param state
     * @return
     * @throws CompletionRecursionException
     * @throws MisconfigurationException
     */
    //    public abstract IToken[] getCompletionsForToken(File file, IDocument doc, ICompletionState state) throws CompletionRecursionException, MisconfigurationException;
    //    Clients must now do the createModule part themselves (and call the getCompletionsForModule)
    //    This is because some places were creating the module more than once from the request, so, now the request
    //    creates the module and caches it.
    //    IModule module = createModule(file, doc, state, this);
    //    return getCompletionsForModule(module, state, true, true);

    /**
     * @return the module with the specified name.
     */
    public abstract IModule getModule(String name, IPythonNature nature, boolean dontSearchInit,
            boolean lookingForRelative);

    public abstract IModule getModule(String name, IPythonNature nature, boolean dontSearchInit);

    /**
     * @return tuple with:
     * 0: mod
     * 1: tok (string)
     * 2: actual tok
     * @throws CompletionRecursionException
     */
    public abstract Tuple3<IModule, String, IToken> findOnImportedMods(ICompletionState state, IModule current)
            throws CompletionRecursionException;

    /**
     * This function tries to find some activation token defined in some imported module.
     * @return tuple with: the module and the token that should be used from it.
     *
     * @param this is the activation token we have. It may be a single token or some dotted name.
     *
     * If it is a dotted name, such as testcase.TestCase, we need to match against some import
     * represented as testcase or testcase.TestCase.
     *
     * If a testcase.TestCase matches against some import named testcase, the import is returned and
     * the TestCase is put as the module
     *
     * 0: mod
     * 1: tok (string)
     * 2: actual tok
     * @throws CompletionRecursionException
     */
    public abstract Tuple3<IModule, String, IToken> findOnImportedMods(IToken[] importedModules,
            ICompletionState state, String currentModuleName, IModule current) throws CompletionRecursionException;

    /**
     * Finds the tokens on the given imported modules
     * @throws CompletionRecursionException
     */
    public IToken[] findTokensOnImportedMods(IToken[] importedModules, ICompletionState state, IModule current)
            throws CompletionRecursionException;

    /**
     *
     * @param doc
     * @param line
     * @param col
     * @param activationToken
     * @param qualifier
     * @return
     */
    public abstract IToken[] getCompletionsForToken(IDocument doc, ICompletionState state)
            throws CompletionRecursionException;

    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     * @param col
     * @param line
     */
    public abstract IToken[] getCompletionsForModule(IModule module, ICompletionState state)
            throws CompletionRecursionException;

    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     * @param col
     * @param line
     */
    public abstract IToken[] getCompletionsForModule(IModule module, ICompletionState state,
            boolean searchSameLevelMods)
                    throws CompletionRecursionException;

    public abstract IToken[] getCompletionsForModule(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion) throws CompletionRecursionException;

    /**
     * This method gets the completions for a wild import.
     * They are added to the completions list
     *
     * @param state this is the completion state
     * @param current this is the current module
     * @param completions OUT this is were completions are added.
     * @param wildImport this is the token identifying the wild import
     *
     * @return true if it was able to find the module and get its completions and false otherwise
     */
    public boolean getCompletionsForWildImport(ICompletionState state, IModule current, List<IToken> completions,
            IToken wildImport);

    /**
     * This method returns the python builtins as completions
     *
     * @param state this is the current completion state
     * @param completions OUT this is where the completions are added.
     * @return the same list that has been passed at completions
     */
    public List<IToken> getBuiltinCompletions(ICompletionState state, List<IToken> completions);

    /**
     * This method can get the global completions for a module (the activation token is usually empty in
     * these cases).
     *
     * What it actually should do is getting the completions for the wild imported modules, plus builtins,
     * plus others passed as arguments.
     *
     * @param globalTokens the global tokens found in the module
     * @param importedModules the imported modules
     * @param wildImportedModules the wild imported modules
     * @param state the current completion state
     * @param current the current module
     * @return a list of IToken
     */
    public abstract List<IToken> getGlobalCompletions(IToken[] globalTokens, IToken[] importedModules,
            IToken[] wildImportedModules, ICompletionState state, IModule current);

    /**
     * Fills the HashSet passed with completions for the class passed considering the current local scope.
     *
     * @param module this is the module we're in
     * @param state the state of the completions
     * @param searchSameLevelMods whether we should search imports in the same level (local imports)
     * @param lookForArgumentCompletion whether we should look for a calltip completion
     * @param lookForClass a list of classes that we should look in the local scope to discover tokens
     * @param hashSet the set that will be filled with the tokens
     * @throws CompletionRecursionException
     */
    public void getCompletionsForClassInLocalScope(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion, List<String> lookForClass, HashSet<IToken> hashSet)
                    throws CompletionRecursionException;

    /**
     * Get the actual token representing the tokName in the passed module
     * @param module the module where we're looking
     * @param tokName the name of the token we're looking for
     * @param nature the nature we're looking for
     * @return the actual token in the module (or null if it was not possible to find it).
     * @throws CompletionRecursionException
     */
    public IToken getRepInModule(IModule module, String tokName, IPythonNature nature)
            throws CompletionRecursionException;

    /**
     * This method gathers an IToken correspondent to the actual token for some import
     *
     * @param state the current completion state
     * @param imported the token generated from an ImportFrom
     * @return the IToken: the actual token that generated that import or the import passed if we weren't
     * able to find its actual definition
     * @throws CompletionRecursionException
     */
    public ImmutableTuple<IModule, IToken> resolveImport(ICompletionState state, IToken imported, IModule current)
            throws CompletionRecursionException;

    /**
     * @return object to lock on.
     */
    public Object getLock();

    /**
     * Finds a module from the string representation of that module.
     *
     * @param fromImportStr the module we want (i.e.: pack1.pack2.other_module or other_module)
     * @param currentModule the module we're in right now (i.e.: pack1.pack2.mod1)
     *
     * @return null (if not found) or a tuple with the module which we could find and a representation found inside that module.
     *
     * Example 1:
     * If we were in a module pack1.pack2.mod1 and were looking for a module 'other_module' which was a relative
     * module and it existed, we'd get the module pack1.pack2.other_module and an empty string, as we were able to find
     * the module.
     *
     * Example 2:
     * If we had a representation pack1.other_module and other_module didn't exist under pack1, we'd get a module
     * 'pack1.__init__' and the string as 'other_module'
     *
     * @throws CompletionRecursionException
     * @throws MisconfigurationException
     */
    public Tuple<IModule, String> findModule(String fromImportStr, String currentModule, ICompletionState state,
            IModule current) throws CompletionRecursionException, MisconfigurationException;

    /**
     * @param astOutputFile
     */
    public void saveToFile(File astOutputFile);

    public abstract IToken[] getCompletionsUnpackingObject(IModule module, ICompletionState copy, ILocalScope scope,
            UnpackInfo unpackPos) throws CompletionRecursionException;

    public IToken[] getCompletionsFromTokenInLocalScope(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion, ILocalScope localScope)
                    throws CompletionRecursionException;

}