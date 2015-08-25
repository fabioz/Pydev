/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ReturnVisitor;
import org.python.pydev.parser.visitors.scope.YieldVisitor;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.ImmutableTuple;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

public abstract class AbstractASTManager implements ICodeCompletionASTManager {

    public static final IToken[] EMPTY_ITOKEN_ARRAY = new IToken[0];

    private static final boolean DEBUG_CACHE = false;

    private final AssignAnalysis assignAnalysis = new AssignAnalysis();

    public AbstractASTManager() {
    }

    private final Object lock = new Object();

    public Object getLock() {
        return lock;
    }

    /**
     * This is the guy that will handle project things for us
     */
    public volatile IModulesManager modulesManager;

    public IModulesManager getModulesManager() {
        return modulesManager;
    }

    /**
     * Set the nature this ast manager works with (if no project is available and a nature is).
     */
    public void setNature(IPythonNature nature) {
        getModulesManager().setPythonNature(nature);
    }

    public IPythonNature getNature() {
        return getModulesManager().getNature();
    }

    public abstract void setProject(IProject project, IPythonNature nature, boolean restoreDeltas);

    public abstract void rebuildModule(File file, ICallback0<IDocument> doc, IProject project,
            IProgressMonitor monitor, IPythonNature nature);

    public abstract void removeModule(File file, IProject project, IProgressMonitor monitor);

    /**
     * Returns the imports that start with a given string. The comparison is not case dependent. Passes all the modules in the cache.
     *
     * @param original is the name of the import module eg. 'from toimport import ' would mean that the original is 'toimport'
     * or something like 'foo.bar' or an empty string (if only 'import').
     * @return a Set with the imports as tuples with the name, the docstring.
     * @throws CompletionRecursionException
     * @throws MisconfigurationException
     */
    public IToken[] getCompletionsForImport(ImportInfo importInfo, ICompletionRequest r, boolean onlyGetDirectModules)
            throws CompletionRecursionException, MisconfigurationException {

        String original = importInfo.importsTipperStr;
        String afterDots = null;
        int level = 0; //meaning: no absolute import

        boolean onlyDots = true;
        if (original.startsWith(".")) {
            //if the import has leading dots, this means it is something like
            //from ...bar import xxx (new way to express the relative import)
            for (int i = 0; i < original.length(); i++) {
                if (original.charAt(i) != '.') {
                    onlyDots = false;
                    afterDots = original.substring(i);
                    break;
                }
                //add one to the relative import level
                level++;
            }
        }
        ICompletionRequest request = r;
        IPythonNature nature = request.getNature();

        String relative = null;
        String moduleName = null;
        if (request.getEditorFile() != null) {
            moduleName = modulesManager.resolveModule(FileUtils.getFileAbsolutePath(request.getEditorFile()));
            if (moduleName != null) {

                if (level > 0) {
                    //ok, it is the import added on python 2.5 (from .. import xxx)
                    List<String> moduleParts = StringUtils.dotSplit(moduleName);
                    if (moduleParts.size() > level) {
                        relative = FullRepIterable.joinParts(moduleParts, moduleParts.size() - level);
                    }

                    if (!onlyDots) {
                        //ok, we have to add the other part too, as we have more than the leading dots
                        //from ..bar import
                        relative += "." + afterDots;
                    }

                } else {
                    boolean isAbsoluteImportEnabled = isAbsoluteImportEnabled(request, nature);

                    if (!isAbsoluteImportEnabled) {
                        String tail = FullRepIterable.headAndTail(moduleName)[0];
                        if (original.length() > 0) {
                            relative = tail + "." + original;
                        } else {
                            relative = tail;
                        }
                    }
                }
            }
        }

        //set to hold the completion (no duplicates allowed).
        Set<IToken> set = new HashSet<IToken>();

        String absoluteModule = original;
        if (absoluteModule.endsWith(".")) {
            absoluteModule = absoluteModule.substring(0, absoluteModule.length() - 1); //remove last char
        }

        //If we have a relative import, first match with the relative and only try to match the absolute if the relative
        //was not found.
        if (relative != null && relative.equals(absoluteModule) == false) {
            getAbsoluteImportTokens(relative, set, IToken.TYPE_RELATIVE_IMPORT, false, importInfo,
                    onlyGetDirectModules);
            if (importInfo.hasImportSubstring) {
                getTokensForModule(relative, nature, relative, set);
            }
        }

        if (set.size() == 0 || absoluteModule.length() == 0 //In the case of an "import zi", the absoluteModule will be an empty string, and we have to get the roots in the completion!
        ) {
            if (level == 0) {
                //first we get the imports... that complete for the token.
                getAbsoluteImportTokens(absoluteModule, set, IToken.TYPE_IMPORT, false, importInfo,
                        onlyGetDirectModules);

                //Now, if we have an initial module, we have to get the completions
                //for it.
                getTokensForModule(original, nature, absoluteModule, set);
            }
        }

        if (level == 1 && moduleName != null) {
            //has returned itself, so, let's remove it
            String strToRemove = FullRepIterable.getLastPart(moduleName);
            for (Iterator<IToken> it = set.iterator(); it.hasNext();) {
                IToken o = it.next();
                if (o.getRepresentation().equals(strToRemove)) {
                    it.remove();
                    //don't break because the token might be different, but not the representation...
                }
            }
        }
        return set.toArray(EMPTY_ITOKEN_ARRAY);
    }

    private boolean isAbsoluteImportEnabled(IModule module, IPythonNature nature) throws MisconfigurationException {
        boolean isAbsoluteImportEnabled = false;
        try {
            isAbsoluteImportEnabled = nature.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
        } catch (MisconfigurationException e) {
            Log.log(e);
        }
        if (!isAbsoluteImportEnabled) {
            //Let's check in Python 2.x if the from __future__ import absolute_import is there.
            if (module != null) {
                isAbsoluteImportEnabled = module.hasFutureImportAbsoluteImportDeclared();
            }
        }
        return isAbsoluteImportEnabled;
    }

    private boolean isAbsoluteImportEnabled(ICompletionRequest request, IPythonNature nature)
            throws MisconfigurationException {
        boolean isAbsoluteImportEnabled = false;
        try {
            isAbsoluteImportEnabled = nature.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
        } catch (MisconfigurationException e) {
            Log.log(e);
        }
        if (!isAbsoluteImportEnabled) {
            //Let's check in Python 2.x if the from __future__ import absolute_import is there.
            IModule module = request.getModule();
            if (module != null) {
                isAbsoluteImportEnabled = module.hasFutureImportAbsoluteImportDeclared();
            }
        }
        return isAbsoluteImportEnabled;
    }

    /**
     * @param moduleToGetTokensFrom the string that represents the token from where we are getting the imports
     * @param set the set where the tokens should be added
     * @param importInfo if null, only the 1st element of the module will be added, otherwise, it'll check the info
     * to see if it should add only the 1st element of the module or the complete module (e.g.: add only xml or
     * xml.dom and other submodules too)
     */
    public void getAbsoluteImportTokens(String moduleToGetTokensFrom, Set<IToken> inputOutput, int type,
            boolean onlyFilesOnSameLevel, ImportInfo importInfo, boolean onlyGetDirectModules) {

        //        boolean getSubModules = false;
        //        if(importInfo != null){
        //            //we only want to get submodules if we're in:
        //            //from xxx
        //            //import xxx
        //            //
        //            //We do NOT want to get it on:
        //            //from xxx import yyy
        //            if(importInfo.hasFromSubstring != importInfo.hasImportSubstring){
        //                getSubModules = true;
        //            }
        //        }

        HashMap<String, IToken> temp = new HashMap<String, IToken>();
        SortedMap<ModulesKey, ModulesKey> modulesStartingWith;
        if (onlyGetDirectModules) {
            modulesStartingWith = modulesManager.getAllDirectModulesStartingWith(moduleToGetTokensFrom);
        } else {
            modulesStartingWith = modulesManager.getAllModulesStartingWith(moduleToGetTokensFrom);
        }

        Iterator<ModulesKey> itModules = modulesStartingWith.keySet().iterator();
        while (itModules.hasNext()) {
            ModulesKey key = itModules.next();

            String element = key.name;
            //            if (element.startsWith(moduleToGetTokensFrom)) { we don't check that anymore because we get all the modules starting with it already
            if (onlyFilesOnSameLevel && key.file != null && key.file.isDirectory()) {
                continue; // we only want those that are in the same directory, and not in other directories...
            }
            element = element.substring(moduleToGetTokensFrom.length());

            //we just want those that are direct
            //this means that if we had initially element = testlib.unittest.anothertest
            //and element became later = .unittest.anothertest, it will be ignored (we
            //should only analyze it if it was something as testlib.unittest and became .unittest
            //we only check this if we only want file modules (in
            if (onlyFilesOnSameLevel && StringUtils.countChars('.', element) > 1) {
                continue;
            }

            boolean goForIt = false;
            //if initial is not empty only get those that start with a dot (submodules, not
            //modules that start with the same name).
            //e.g. we want xml.dom
            //and not xmlrpclib
            //if we have xml token (not using the qualifier here)
            if (moduleToGetTokensFrom.length() != 0) {
                if (element.length() > 0 && element.charAt(0) == ('.')) {
                    element = element.substring(1);
                    goForIt = true;
                }
            } else {
                goForIt = true;
            }

            if (element.length() > 0 && goForIt) {
                List<String> splitted = StringUtils.dotSplit(element);
                if (splitted.size() > 0) {
                    String strToAdd;

                    strToAdd = splitted.get(0);
                    //                        if(!getSubModules){
                    //                        }else{
                    //                            if(element.endsWith(".__init__")){
                    //                                strToAdd = element.substring(0, element.length()-9);
                    //                            }else{
                    //                                strToAdd = element;
                    //                            }
                    //                        }
                    //this is the completion
                    temp.put(strToAdd, new ConcreteToken(strToAdd, "", "", moduleToGetTokensFrom, type));
                }
            }
            //            }
        }
        inputOutput.addAll(temp.values());
    }

    /**
     * @param original this is the initial module where the completion should happen (may have class in it too)
     * @param moduleToGetTokensFrom
     * @param set set where the tokens should be added
     * @throws CompletionRecursionException
     */
    protected void getTokensForModule(String original, IPythonNature nature, String moduleToGetTokensFrom,
            Set<IToken> set) throws CompletionRecursionException {
        if (moduleToGetTokensFrom.length() > 0) {
            if (original.endsWith(".")) {
                original = original.substring(0, original.length() - 1);
            }

            Tuple<IModule, String> modTok = findModuleFromPath(original, nature, false, null); //the current module name is not used as it is not relative
            IModule m = modTok.o1;
            String tok = modTok.o2;

            if (m == null) {
                //we were unable to find it with the given path, so, there's nothing else to do here...
                return;
            }

            IToken[] globalTokens;
            if (tok != null && tok.length() > 0) {
                CompletionState state2 = new CompletionState(-1, -1, tok, nature, "");
                state2.setBuiltinsGotten(true); //we don't want to get builtins here
                globalTokens = m.getGlobalTokens(state2, this);
            } else {
                CompletionState state2 = new CompletionState(-1, -1, "", nature, "");
                state2.setBuiltinsGotten(true); //we don't want to get builtins here
                globalTokens = getCompletionsForModule(m, state2);
            }

            for (int i = 0; i < globalTokens.length; i++) {
                IToken element = globalTokens[i];
                //this is the completion
                set.add(element);
            }
        }
    }

    /**
     * @param file
     * @param doc
     * @param state
     * @return
     * @throws MisconfigurationException
     */
    public static IModule createModule(File file, IDocument doc, IPythonNature nature)
            throws MisconfigurationException {
        return AbstractModule.createModuleFromDoc(file, doc, nature);
    }

    //    /**
    //     * @throws MisconfigurationException
    //     * @see org.python.pydev.core.ICodeCompletionASTManager#getCompletionsForToken(java.io.File, org.eclipse.jface.text.IDocument, org.python.pydev.editor.codecompletion.revisited.CompletionState)
    //     */
    //    public IToken[] getCompletionsForToken(File file, IDocument doc, ICompletionState state) throws CompletionRecursionException, MisconfigurationException {
    //        IModule module = createModule(file, doc, state, this);
    //        return getCompletionsForModule(module, state, true, true);
    //    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForToken(org.eclipse.jface.text.IDocument, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForToken(IDocument doc, ICompletionState state) {
        IToken[] completionsForModule;
        try {
            ParseOutput obj = PyParser
                    .reparseDocument(new PyParser.ParserInfo(doc, state.getNature()));
            SimpleNode n = (SimpleNode) obj.ast;
            IModule module = AbstractModule.createModule(n);

            completionsForModule = getCompletionsForModule(module, state, true, true);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                if (e instanceof NullPointerException) {
                    Log.log(e);
                    message = "NullPointerException";
                } else {
                    message = "Null error message";
                }
            }
            completionsForModule = new IToken[] { new ConcreteToken(message, message, "", "", IToken.TYPE_UNKNOWN) };
        }

        return completionsForModule;
    }

    /**
     * By default does not look for relative import
     */
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return modulesManager.getModule(name, nature, dontSearchInit, false);
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     *
     * @param name the name of the module we're looking for
     * @param lookingForRelative determines whether we're looking for a relative module (in which case we should
     * not check in other places... only in the module)
     * @return the module represented by this name
     */
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit, boolean lookingForRelative) {
        if (lookingForRelative) {
            return modulesManager.getRelativeModule(name, nature);
        } else {
            return modulesManager.getModule(name, nature, dontSearchInit);
        }
    }

    /**
     * Identifies the token passed and if it maps to a builtin not 'easily recognizable', as
     * a string or list, we return it.
     *
     * @param state
     * @return
     */
    protected IToken[] getBuiltinsCompletions(ICompletionState state) {
        ICompletionState state2 = state.getCopy();

        String act = state.getActivationToken();

        //check for the builtin types.
        state2.setActivationToken(NodeUtils.getBuiltinType(act));

        if (state2.getActivationToken() != null) {
            IModule m = getBuiltinMod(state.getNature());
            if (m != null) {
                return m.getGlobalTokens(state2, this);
            }
        }

        if (act.equals("__builtins__") || act.startsWith("__builtins__.")) {
            act = act.substring(12);
            if (act.startsWith(".")) {
                act = act.substring(1);
            }
            IModule m = getBuiltinMod(state.getNature());
            ICompletionState state3 = state.getCopy();
            state3.setActivationToken(act);
            return m.getGlobalTokens(state3, this);
        }
        return null;
    }

    /**
     * @throws CompletionRecursionException
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForModule(org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForModule(IModule module, ICompletionState state)
            throws CompletionRecursionException {
        return getCompletionsForModule(module, state, true);
    }

    /**
     * @throws CompletionRecursionException
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForModule(org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.CompletionState, boolean)
     */
    public IToken[] getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods)
            throws CompletionRecursionException {
        return getCompletionsForModule(module, state, true, false);
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForModule(org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.CompletionState, boolean, boolean)
     */
    public IToken[] getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion) throws CompletionRecursionException {
        return getCompletionsForModule(module, state, searchSameLevelMods, lookForArgumentCompletion, false);
    }

    /**
     * @see #getCompletionsForModule(IModule, ICompletionState, boolean, boolean)
     *
     * Same thing but may handle things as if it was a wild import (in which case, the tokens starting with '_' are
     * removed and if __all__ is available, only the tokens contained in __all__ are returned)
     */
    public IToken[] getCompletionsForModule(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion, boolean handleAsWildImport) throws CompletionRecursionException {
        String name = module.getName();
        Object key = new TupleN("getCompletionsForModule", name != null ? name : "", state.getActivationToken(),
                searchSameLevelMods, lookForArgumentCompletion, state.getBuiltinsGotten(),
                state.getLocalImportsGotten(), handleAsWildImport);

        IToken[] ret = (IToken[]) state.getObj(key);
        if (ret != null) {
            if (DEBUG_CACHE) {
                System.out.println("Checking if cache is correct for: " + key);
                IToken[] internal = internalGenerateGetCompletionsForModule(module, state, searchSameLevelMods,
                        lookForArgumentCompletion);
                internal = filterForWildImport(module, handleAsWildImport, internal);
                //the new request may actually have no tokens if a completion exception occurred.
                if (internal.length != 0 && ret.length != internal.length) {
                    throw new RuntimeException("This can't happen... it should always return the same completions!");
                }
            }
            return ret;
        }

        IToken[] completionsForModule = internalGenerateGetCompletionsForModule(module, state, searchSameLevelMods,
                lookForArgumentCompletion);
        completionsForModule = filterForWildImport(module, handleAsWildImport, completionsForModule);

        state.add(key, completionsForModule);
        return completionsForModule;
    }

    /**
     * Filters the tokens according to the wild import rules:
     * - the tokens starting with '_' are removed
     * - if __all__ is available, only the tokens contained in __all__ are returned)
     */
    private IToken[] filterForWildImport(IModule module, boolean handleAsWildImport, IToken[] completionsForModule) {
        if (module != null && handleAsWildImport) {
            ArrayList<IToken> ret = new ArrayList<IToken>();

            for (int j = 0; j < completionsForModule.length; j++) {
                IToken token = completionsForModule[j];
                //on wild imports we don't get names that start with '_'
                if (!token.getRepresentation().startsWith("_")) {
                    ret.add(token);
                }
            }

            if (module instanceof SourceModule) {
                //Support for __all__: filter things if __all__ is available.
                SourceModule sourceModule = (SourceModule) module;
                GlobalModelVisitor globalModelVisitorCache = sourceModule.getGlobalModelVisitorCache();
                if (globalModelVisitorCache != null) {
                    globalModelVisitorCache.filterAll(ret);
                }
            }
            return ret.toArray(new IToken[ret.size()]);
        } else {
            return completionsForModule;
        }
    }

    private void log(String message, IModule module, ICompletionState state) {
        String name;
        if (module == null) {
            name = "null module";
        } else {
            name = module.getName();
        }
        Log.toLogFile(this, message + ": " + name + " -- " + state.getActivationToken());
    }

    /**
     * This method should only be accessed from the public getCompletionsForModule (which caches the result).
     */
    private IToken[] internalGenerateGetCompletionsForModule(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion) throws CompletionRecursionException {

        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            log("internalGenerateGetCompletionsForModule", module, state);
        }
        state.checkMaxTimeForCompletion();

        ArrayList<IToken> importedModules = new ArrayList<IToken>();

        ILocalScope localScope = null;
        int line = state.getLine();
        int col = state.getCol();

        if (state.getLocalImportsGotten() == false) {
            //in the first analyzed module, we have to get the local imports too.
            state.setLocalImportsGotten(true);
            if (module != null && line >= 0) {
                localScope = module.getLocalScope(line, col);
                if (localScope != null) {
                    importedModules.addAll(localScope.getLocalImportedModules(line + 1, col + 1, module.getName()));
                }
            }
        }

        IToken[] builtinsCompletions = getBuiltinsCompletions(state);
        if (builtinsCompletions != null) {
            return builtinsCompletions;
        }

        String act = state.getActivationToken();
        int parI = act.indexOf('(');
        if (parI != -1) {
            state.setFullActivationToken(act);
            act = act.substring(0, parI);
            state.setActivationToken(act);
            state.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
        }

        if (module != null) {

            //get the tokens (global, imported and wild imported)
            IToken[] globalTokens = module.getGlobalTokens();

            List<IToken> tokenImportedModules = Arrays.asList(module.getTokenImportedModules());
            importedModules.addAll(tokenImportedModules);
            state.setTokenImportedModules(importedModules);
            IToken[] wildImportedModules = module.getWildImportedModules();

            //now, lets check if this is actually a module that is an __init__ (if so, we have to get all
            //the other .py files as modules that are in the same level as the __init__)
            Set<IToken> initial = new HashSet<IToken>();
            if (searchSameLevelMods) {
                //now, we have to ask for the module if it's a 'package' (folders that have __init__.py for python
                //or only folders -- not classes -- in java).
                if (module.isPackage()) {
                    HashSet<IToken> gotten = new HashSet<IToken>();
                    //the module also decides how to get its submodules
                    getAbsoluteImportTokens(module.getPackageFolderName(), gotten, IToken.TYPE_IMPORT, true, null,
                            false);
                    for (IToken token : gotten) {
                        if (token.getRepresentation().equals("__init__") == false) {
                            initial.add(token);
                        }
                    }
                }
            }

            if (state.getActivationToken().length() == 0) {

                List<IToken> completions = getGlobalCompletions(globalTokens,
                        importedModules.toArray(EMPTY_ITOKEN_ARRAY), wildImportedModules, state, module);

                //now find the locals for the module
                if (line >= 0) {
                    IToken[] localTokens = module.getLocalTokens(line, col, localScope);
                    for (int i = 0; i < localTokens.length; i++) {
                        completions.add(localTokens[i]);
                    }
                }
                completions.addAll(initial); //just add all that are in the same level if it was an __init__

                return completions.toArray(EMPTY_ITOKEN_ARRAY);

            } else { //ok, we have a token, find it and get its completions.

                //first check if the token is a module... if it is, get the completions for that module.
                IToken[] tokens = findTokensOnImportedMods(importedModules.toArray(EMPTY_ITOKEN_ARRAY), state, module);
                if (tokens != null && tokens.length > 0) {
                    return decorateWithLocal(tokens, localScope, state);
                }

                //if it is an __init__, modules on the same level are treated as local tokens
                if (searchSameLevelMods) {
                    tokens = searchOnSameLevelMods(initial, state);
                    if (tokens != null && tokens.length > 0) {
                        return decorateWithLocal(tokens, localScope, state);
                    }
                }

                //for wild imports, we must get the global completions with __all__ filtered
                //wild imports: recursively go and get those completions and see if any matches it.
                for (int i = 0; i < wildImportedModules.length; i++) {

                    IToken name = wildImportedModules[i];
                    IModule mod = getModule(name.getAsRelativeImport(module.getName()), state.getNature(), false); //relative (for wild imports this is ok... only a module can be used in wild imports)

                    if (mod == null) {
                        mod = getModule(name.getOriginalRep(), state.getNature(), false); //absolute
                    }

                    if (mod != null) {
                        state.checkFindModuleCompletionsMemory(mod, state.getActivationToken());
                        IToken[] completionsForModule = getCompletionsForModule(mod, state);
                        if (completionsForModule.length > 0) {
                            return decorateWithLocal(completionsForModule, localScope, state);
                        }
                    } else {
                        //"Module not found:" + name.getRepresentation()
                    }
                }

                //it was not a module (would have returned already), so, try to get the completions for a global token defined.
                tokens = module.getGlobalTokens(state, this);
                if (tokens.length > 0) {
                    return decorateWithLocal(tokens, localScope, state);
                }

                //If it was still not found, go to builtins.
                IModule builtinsMod = getBuiltinMod(state.getNature());
                if (builtinsMod != null && builtinsMod != module) {
                    tokens = getCompletionsForModule(builtinsMod, state);
                    if (tokens.length > 0) {
                        if (tokens[0].getRepresentation().equals("ERROR:") == false) {
                            return decorateWithLocal(tokens, localScope, state);
                        }
                    }
                }

                //Let's check if we have to unpack it...
                int oldLookingFor = state.getLookingFor();
                state.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE, true);
                try {
                    if (state.getActivationToken().endsWith(".__getitem__")) {
                        String activationToken = state.getActivationToken();
                        String compoundActivationToken = activationToken.substring(0, activationToken.length() - 12);

                        IToken[] ret = getCompletionsUnpackingObject(module,
                                state.getCopyWithActTok(compoundActivationToken), localScope, new UnpackInfo());
                        if (ret != null && ret.length > 0) {
                            return ret;
                        }
                    } else {
                        if (localScope != null) {
                            ISimpleNode foundAtASTNode = localScope.getFoundAtASTNode();
                            if (foundAtASTNode instanceof For) {
                                For for1 = (For) foundAtASTNode;

                                // case where we may have to unpack some iteration
                                // e.g.: for a, b in x.items():
                                IToken[] ret = getCompletionsUnpackingForLoop(module, state, localScope, for1);
                                if (ret != null && ret.length > 0) {
                                    return ret;
                                }
                                //Note: we don't bail out here because it's possible that the user has
                                //added the type on the context (because on a for unpacking either we find it
                                //when checking the for loop unpack or the user has to explicitly give
                                //us a hint).
                            }
                        }
                    }
                } finally {
                    state.setLookingFor(oldLookingFor, true);
                }

                if (lookForArgumentCompletion && localScope != null) {

                    IToken[] ret = getCompletionsFromTokenInLocalScope(module, state, searchSameLevelMods,
                            lookForArgumentCompletion,
                            localScope);
                    if (ret != null && ret.length > 0) {
                        return ret;
                    }
                }

                //nothing worked so far, so, let's look for an assignment...
                return getAssignCompletions(module, state, lookForArgumentCompletion, localScope);
            }

        } else {
            Log.log("Module passed in is null!!");
        }

        return EMPTY_ITOKEN_ARRAY;
    }

    public IToken[] getCompletionsFromTokenInLocalScope(IModule module, ICompletionState state,
            boolean searchSameLevelMods, boolean lookForArgumentCompletion, ILocalScope localScope)
                    throws CompletionRecursionException {
        IToken[] tokens;
        //now, if we have to look for arguments and search things in the local scope, let's also
        //check for assert (isinstance...) in this scope with the given variable.
        List<String> lookForClass = localScope.getPossibleClassesForActivationToken(state
                .getActivationToken());
        if (lookForClass.size() > 0) {
            List<String> lst = new ArrayList<>(lookForClass.size());
            for (String s : lookForClass) {
                lst.add(NodeUtils.getPackedTypeFromDocstring(s));
            }
            lookForClass = lst;
            HashSet<IToken> hashSet = new HashSet<IToken>();

            getCompletionsForClassInLocalScope(module, state, searchSameLevelMods,
                    lookForArgumentCompletion, lookForClass, hashSet);

            if (hashSet.size() > 0) {
                return hashSet.toArray(EMPTY_ITOKEN_ARRAY);
            } else {
                //Give a chance to find it without the scope
                //Try to deal with some token that's not imported
                IToken[] ret = getCompletionsFromTypeRepresentation(state, lookForClass, module);
                if (ret != null && ret.length > 0) {
                    return ret;
                }
            }
        }

        //ok, didn't find in assert isinstance... keep going
        //if there was no assert for the class, get from extensions / local scope interface
        tokens = CompletionParticipantsHelper.getCompletionsForMethodParameter(state, localScope).toArray(
                EMPTY_ITOKEN_ARRAY);
        if (tokens != null && tokens.length > 0) {
            return tokens;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public IToken[] getCompletionsFromTypeRepresentation(ICompletionState state, List<String> lookForClass,
            IModule currentModule)
                    throws CompletionRecursionException {
        state.checkMaxTimeForCompletion();

        //First check in the current module...
        for (String classToCheck : lookForClass) {
            IToken[] completionsForModule = getCompletionsForModule(
                    currentModule,
                    state.getCopyWithActTok(classToCheck));
            if (completionsForModule != null
                    && completionsForModule.length > 0) {
                return completionsForModule;
            }
        }

        List<IPyDevCompletionParticipant> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_COMPLETION);

        for (String classToCheck : lookForClass) {
            for (IPyDevCompletionParticipant participant : participants) {
                ICompletionState copy = state.getCopyWithActTok(classToCheck);
                int oldLookingFor = copy.getLookingFor();
                copy.setLookingFor(ICompletionState.LOOKING_FOR_ASSIGN);

                Collection<IToken> collection = participant.getCompletionsForType(copy);
                if (collection != null && collection.size() > 0) {
                    return collection.toArray(EMPTY_ITOKEN_ARRAY);
                }
                //If it didn't return, restore the old value...
                copy.setLookingFor(oldLookingFor);
            }
        }
        return EMPTY_ITOKEN_ARRAY;
    }

    private IToken[] getCompletionsUnpackingForLoop(IModule module, ICompletionState state, ILocalScope localScope,
            For for1)
                    throws CompletionRecursionException {
        state.checkMaxTimeForCompletion();
        if (for1.target instanceof org.python.pydev.parser.jython.ast.Tuple
                || for1.target instanceof org.python.pydev.parser.jython.ast.List) {
            exprType[] eltsTarget = NodeUtils.getEltsFromCompoundObject(for1.target);
            if (eltsTarget != null) {
                UnpackInfo unpackPos = new UnpackInfo();
                unpackPos.addUnpackFor();
                for (int i = 0; i < eltsTarget.length; i++) {
                    exprType elt = eltsTarget[i];
                    if (state.getActivationToken().equals(
                            NodeUtils.getFullRepresentationString(elt))) {
                        unpackPos.addUnpackTuple(i);
                        break;
                    }
                }

                boolean hasUnpackInfo = unpackPos.hasUnpackInfo();
                if (hasUnpackInfo) {
                    exprType[] elts = NodeUtils.getEltsFromCompoundObject(for1.iter);
                    if (elts != null) {
                        if (elts.length == 1) {
                            if (elts[0] instanceof org.python.pydev.parser.jython.ast.Tuple
                                    || elts[0] instanceof org.python.pydev.parser.jython.ast.List) {
                                elts = NodeUtils.getEltsFromCompoundObject(elts[0]);
                            }
                        }
                        int unpackTuple = unpackPos.getUnpackTuple(elts.length);
                        if (unpackTuple >= 0) {
                            String rep = NodeUtils.getFullRepresentationString(elts[unpackTuple]);
                            if (rep != null) {
                                ICompletionState copyWithActTok = state.getCopyWithActTok(rep);
                                if (elts[unpackTuple] instanceof Call) {
                                    copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                                }
                                IToken[] completionsForModule = getCompletionsForModule(module,
                                        copyWithActTok);
                                if (completionsForModule.length > 0) {
                                    return completionsForModule;
                                }
                            }
                        }
                    } else {
                        IToken[] ret = getDictCompletionOnForLoop(module, state, for1, localScope, unpackPos);
                        if (ret != null && ret.length > 0) {
                            return ret;
                        }
                    }
                }
            }

        } else if (state.getActivationToken().equals(
                NodeUtils.getFullRepresentationString(for1.target))) {
            // We're the target of some for loop, so, in fact, we're unpacking some compound object...
            if (for1.iter != null) {
                IToken[] ret = null;
                exprType[] elts = NodeUtils.getEltsFromCompoundObject(for1.iter);
                if (elts != null) {
                    UnpackInfo unpackInfo = new UnpackInfo();
                    unpackInfo.addUnpackFor();
                    ret = getCompletionsFromUnpackedCompoundObject(module, state, elts, unpackInfo);
                } else {
                    String rep = NodeUtils
                            .getFullRepresentationString(for1.iter);
                    if (rep != null) {
                        ret = getCompletionsUnpackingObject(module,
                                state.getCopyWithActTok(rep),
                                localScope, new UnpackInfo(true, -1));
                    }
                }
                if (ret != null && ret.length > 0) {
                    return ret;
                }
                // Check if we're doing some keys/values/items in a dict...
                UnpackInfo unpackPos = new UnpackInfo();
                unpackPos.addUnpackFor();
                ret = getDictCompletionOnForLoop(module, state, for1, localScope, unpackPos);
                if (ret != null && ret.length > 0) {
                    return ret;
                }
            }
        }
        return null;
    }

    private IToken[] getDictCompletionOnForLoop(IModule module, ICompletionState state, For for1,
            ILocalScope localScope, UnpackInfo unpackPos)
                    throws CompletionRecursionException {
        state.checkMaxTimeForCompletion();

        exprType func = null;
        if (for1.iter instanceof Call) {
            Call call = (Call) for1.iter;
            func = call.func;
        } else if (for1.iter instanceof exprType) {
            func = for1.iter;
        }
        if (func instanceof Attribute) {
            Attribute attribute = (Attribute) func;
            String representationString = NodeUtils
                    .getFullRepresentationString(attribute.attr);
            if (representationString != null) {
                exprType value = attribute.value;
                if (value != null) {

                    int searchDict = -1;
                    if ("keys".equals(representationString)
                            || "iterkeys".equals(representationString)) {
                        searchDict = 0;
                    }
                    if ("values".equals(representationString)
                            || "itervalues".equals(representationString)) {
                        searchDict = 1;
                    }
                    if ("items".equals(representationString)
                            || "iteritems".equals(representationString)) {
                        searchDict = 2;
                    }
                    if (searchDict >= 0) {
                        String rep = NodeUtils.getFullRepresentationString(value);
                        try {
                            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
                            PyRefactoringFindDefinition.findActualDefinition(null, module,
                                    rep,
                                    selected,
                                    value.beginLine, value.beginLine, state.getNature(),
                                    state);
                            for (Iterator<IDefinition> iterator = selected.iterator(); iterator
                                    .hasNext();) {
                                IDefinition iDefinition = iterator.next();
                                // Ok, couldn't get possible classes, let's see if the definition is a dict
                                if (iDefinition instanceof AssignDefinition) {
                                    AssignDefinition assignDefinition = (AssignDefinition) iDefinition;
                                    IToken[] tokens = getCompletionsFromAssignDefinition(module, state,
                                            unpackPos, assignDefinition);
                                    if (tokens != null && tokens.length > 0) {
                                        return tokens;
                                    }
                                }

                                if (iDefinition instanceof Definition) {
                                    Definition definition = (Definition) iDefinition;
                                    if (definition.scope != null) {
                                        List<String> possibleClassesForActivationToken = definition.scope
                                                .getPossibleClassesForActivationToken(rep);
                                        for (String string : possibleClassesForActivationToken) {
                                            String unpackedTypeFromDocstring = null;
                                            if (searchDict == 0) {
                                                unpackedTypeFromDocstring = NodeUtils
                                                        .getUnpackedTypeFromTypeDocstring(string,
                                                                new UnpackInfo(true, 0));
                                            } else if (searchDict == 1) {
                                                unpackedTypeFromDocstring = NodeUtils
                                                        .getUnpackedTypeFromTypeDocstring(string,
                                                                new UnpackInfo(true, 1));
                                            } else if (searchDict == 2) {
                                                unpackedTypeFromDocstring = NodeUtils
                                                        .getUnpackedTypeFromTypeDocstring(string,
                                                                unpackPos);
                                            }
                                            if (unpackedTypeFromDocstring.equals(string)) {
                                                continue;
                                            }

                                            IToken[] ret = getCompletionsFromTypeRepresentation(
                                                    state, Arrays.asList(unpackedTypeFromDocstring),
                                                    definition.module);
                                            if (ret != null && ret.length > 0) {
                                                return ret;
                                            }
                                        }
                                    }

                                }
                            }
                        } catch (CompletionRecursionException e) {
                            throw e;
                        } catch (Exception e) {
                            Log.log(e);
                            throw new RuntimeException("Error when getting definition for:"
                                    + state.getActivationToken(), e);
                        }
                    }
                }
            }
        }
        //Ok, couldn't get it as a dict, but it may still be some custom class...
        String full = NodeUtils.getFullRepresentationString(for1.iter);
        if (full != null) {
            ICompletionState copyWithActTok = state.getCopyWithActTok(full);
            copyWithActTok.setLine(NodeUtils.getLineDefinition(for1.iter) - 1);
            copyWithActTok.setCol(NodeUtils.getColDefinition(for1.iter) - 1);
            IToken[] ret = getCompletionsUnpackingObject(module, copyWithActTok, localScope, unpackPos);
            if (ret != null && ret.length > 0) {
                return ret;
            }
        }
        return null;
    }

    /**
     * @param unpackPos which position are we unpacking? -1 for 'don't care' (i.e.: for a in b) -- 0 would be for a,b in x (when
     * asking for completions in a).s
     */
    public IToken[] getCompletionsUnpackingObject(IModule module, ICompletionState state, ILocalScope scope,
            UnpackInfo unpackPos)
                    throws CompletionRecursionException {
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
        state.pushGetCompletionsUnpackingObject();
        try {
            PyRefactoringFindDefinition.findActualDefinition(null, module, state.getActivationToken(),
                    selected,
                    state.getLine() + 1, state.getCol() + 1, state.getNature(),
                    state);
            for (Iterator<IDefinition> iterator = selected.iterator(); iterator.hasNext();) {
                IDefinition iDefinition = iterator.next();
                if (!(iDefinition instanceof AssignDefinition) && iDefinition instanceof Definition) {
                    Definition definition = (Definition) iDefinition;
                    if (definition.ast != null) {
                        IToken[] ret = getCompletionsUnpackingAST(definition.ast,
                                definition.module, state, unpackPos);
                        if (ret != null && ret.length > 0) {
                            return ret;
                        }
                    }

                    // If it still hasn't returned, try to get it from the docstring
                    String docstring = definition.getDocstring(this.getNature(), state);
                    if (docstring != null && !docstring.isEmpty()) {
                        IToken[] tokens = getCompletionsUnpackingDocstring(module, state, unpackPos, docstring);
                        if (tokens != null && tokens.length > 0) {
                            return tokens;
                        }
                    }

                } else if (iDefinition instanceof AssignDefinition) {
                    AssignDefinition assignDefinition = (AssignDefinition) iDefinition;
                    IToken[] tokens = getCompletionsFromAssignDefinition(module, state, unpackPos, assignDefinition);
                    if (tokens != null && tokens.length > 0) {
                        return tokens;
                    }
                }
            }
        } catch (CompletionRecursionException e) {
            throw e;
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException("Error when getting definition for:" + state.getActivationToken(), e);
        } finally {
            state.popGetCompletionsUnpackingObject();
        }

        //If we didn't return so far, we should still check for types specified in docstrings...
        if (scope != null) {
            List<String> possibleClassesForActivationToken = scope.getPossibleClassesForActivationToken(state
                    .getActivationToken());

            for (String string : possibleClassesForActivationToken) {
                String unpackedTypeFromDocstring = NodeUtils.getUnpackedTypeFromTypeDocstring(string, unpackPos);
                ICompletionState copyWithActTok = state.getCopyWithActTok(unpackedTypeFromDocstring);
                copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                IToken[] completionsForModule = getCompletionsForModule(module,
                        copyWithActTok);
                if (completionsForModule != null && completionsForModule.length > 0) {
                    return completionsForModule;
                } else {
                    //Try to deal with some token that's not imported
                    List<IPyDevCompletionParticipant> participants = ExtensionHelper
                            .getParticipants(ExtensionHelper.PYDEV_COMPLETION);
                    ArrayList<Object> lst = new ArrayList<>();
                    for (IPyDevCompletionParticipant participant : participants) {
                        Collection<IToken> collection = participant.getCompletionsForType(copyWithActTok);
                        if (collection != null && collection.size() > 0) {
                            lst.addAll(collection);
                        }
                    }
                    if (lst.size() > 0) {
                        return lst.toArray(new IToken[0]);
                    }
                }
            }
        }

        return null;
    }

    private IToken[] getCompletionsFromAssignDefinition(IModule module, ICompletionState state, UnpackInfo unpackPos,
            AssignDefinition assignDefinition) throws CompletionRecursionException, Exception {
        exprType[] elts = NodeUtils.getEltsFromCompoundObject(assignDefinition.nodeValue);
        if (elts != null) {
            // I.e.: something as [1,2,3, Call()]
            IToken[] completionsFromUnpackedList = getCompletionsFromUnpackedCompoundObject(module, state,
                    elts, unpackPos);
            if (completionsFromUnpackedList != null) {
                return completionsFromUnpackedList;
            }
        } else {
            ArrayList<IDefinition> found = new ArrayList<>();
            // Pointing to some other place... let's follow it.
            PyRefactoringFindDefinition.findActualDefinition(null, assignDefinition.module,
                    assignDefinition.value,
                    found,
                    assignDefinition.line, assignDefinition.col, state.getNature(),
                    state);
            for (IDefinition f : found) {
                if (f instanceof Definition) {
                    Definition definition = (Definition) f;
                    if (definition.ast != null) {
                        //We're unpacking some class/method we found... something as:
                        //class SomeClass:
                        //    def __iter__(self):
                        //x = SomeClass()
                        //for a in x:
                        //    a.
                        IToken[] ret = getCompletionsUnpackingAST(definition.ast, definition.module, state,
                                unpackPos);
                        if (ret != null && ret.length > 0) {
                            return ret;
                        }
                    }
                }
            }
        }
        return null;
    }

    private IToken[] getCompletionsUnpackingAST(SimpleNode ast, final IModule module, ICompletionState state,
            UnpackInfo unpackPos)
                    throws CompletionRecursionException {

        if (ast instanceof FunctionDef) {
            IToken[] tokens = getCompletionsUnpackingDocstring(module, state, unpackPos,
                    NodeUtils.getNodeDocString(ast));
            if (tokens != null && tokens.length > 0) {
                return tokens;
            }

            List<Yield> findYields = YieldVisitor.findYields((FunctionDef) ast);
            for (Yield yield : findYields) {
                //Note: the yield means we actually have a generator, so, the value yield is already
                //what we should complete on.
                if (yield.value != null) {
                    String rep = NodeUtils
                            .getFullRepresentationString(yield.value);
                    if (rep != null) {
                        ICompletionState copyWithActTok = state.getCopyWithActTok(rep);
                        copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                        IToken[] completionsForModule = getCompletionsForModule(module,
                                copyWithActTok);
                        if (completionsForModule.length > 0) {
                            return completionsForModule;
                        }
                    }
                }
            }

            List<Return> findReturns = ReturnVisitor.findReturns((FunctionDef) ast);
            for (Return return1 : findReturns) {
                //Return types have to be unpacked...
                if (return1.value != null) {
                    exprType[] elts = NodeUtils.getEltsFromCompoundObject(return1.value);
                    if (elts != null) {
                        IToken[] ret = getCompletionsFromUnpackedCompoundObject(module, state, elts, unpackPos);
                        if (ret != null && ret.length > 0) {
                            return ret;
                        }

                    } else {
                        String rep = NodeUtils.getFullRepresentationString(return1.value);
                        if (rep != null) {
                            IToken[] completionsUnpackingObject = getCompletionsUnpackingObject(module,
                                    state.getCopyWithActTok(rep), null, unpackPos);
                            if (completionsUnpackingObject != null && completionsUnpackingObject.length > 0) {
                                return completionsUnpackingObject;
                            }
                        }
                    }
                }
            }
        } else if (ast instanceof ClassDef) {
            String rep = NodeUtils
                    .getFullRepresentationString(ast);
            if (rep != null) {
                IToken[] completionsForModule = this.getCompletionsForModule(module,
                        state.getCopyWithActTok(rep));
                IToken getItemToken = null;
                for (int i = 0; i < completionsForModule.length; i++) {
                    IToken iToken = completionsForModule[i];

                    switch (iToken.getRepresentation()) {
                        case "__getitem__":
                            getItemToken = iToken;
                            break;
                        case "__iter__":
                            //__iter__ has priority over __getitem__
                            //If we find it we'll try to unpack completions from it.
                            if (iToken instanceof SourceToken) {
                                SourceToken sourceToken = (SourceToken) iToken;
                                IModule useModule = null;
                                if (module.getName().equals(
                                        sourceToken.getParentPackage())) {
                                    useModule = module;
                                }
                                if (useModule == null) {
                                    String parentPackage = sourceToken.getParentPackage();
                                    useModule = getModule(parentPackage, state.getNature(),
                                            true);
                                }

                                IToken[] ret = getCompletionsUnpackingAST(sourceToken.getAst(),
                                        useModule, state, unpackPos);
                                if (ret != null && ret.length > 0) {
                                    return ret;
                                }
                            }
                            break;
                    }
                }
                if (getItemToken instanceof SourceToken) {
                    //The __getitem__ is already unpacked (i.e.: __iter__ returns a generator
                    //and __getitem__ already returns the value we're iterating through).
                    SourceToken sourceToken = (SourceToken) getItemToken;
                    IModule useModule = null;
                    if (module.getName().equals(
                            sourceToken.getParentPackage())) {
                        useModule = module;
                    } else {
                        String parentPackage = getItemToken.getParentPackage();
                        useModule = getModule(parentPackage, state.getNature(), true);
                    }
                    IToken[] ret = getCompletionsNotUnpackingToken(sourceToken,
                            useModule, state);
                    if (ret != null && ret.length > 0) {
                        return ret;
                    }
                }
            }
        }

        return null;
    }

    private IToken[] getCompletionsUnpackingDocstring(final IModule module, ICompletionState state,
            UnpackInfo unpackPos,
            String docstring) throws CompletionRecursionException {
        if (docstring != null) {
            String type = NodeUtils.getReturnTypeFromDocstring(docstring);
            if (type != null) {
                String unpackedTypeFromDocstring = NodeUtils.getUnpackedTypeFromTypeDocstring(type, unpackPos);
                if (unpackedTypeFromDocstring != null) {
                    ICompletionState copyWithActTok = state.getCopyWithActTok(unpackedTypeFromDocstring);
                    copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                    IToken[] completionsForModule = getCompletionsForModule(module,
                            copyWithActTok);
                    if (completionsForModule.length > 0) {
                        return completionsForModule;
                    }
                }
            }
        }
        return null;
    }

    private IToken[] getCompletionsNotUnpackingToken(SourceToken token, IModule useModule, ICompletionState state)
            throws CompletionRecursionException {
        if (useModule == null) {
            String parentPackage = token.getParentPackage();
            useModule = getModule(parentPackage, state.getNature(), true);
        }

        SimpleNode ast = token.getAst();
        if (ast instanceof FunctionDef) {
            String type = NodeUtils.getReturnTypeFromDocstring(ast);
            if (type != null) {
                ICompletionState copyWithActTok = state.getCopyWithActTok(type);
                copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                IToken[] completionsForModule = getCompletionsForModule(useModule,
                        copyWithActTok);
                if (completionsForModule.length > 0) {
                    return completionsForModule;
                }
            }

            List<Return> findReturns = ReturnVisitor.findReturns((FunctionDef) ast);
            for (Return return1 : findReturns) {
                //Return types have to be unpacked...
                if (return1.value != null) {
                    String rep = NodeUtils.getFullRepresentationString(return1.value);
                    if (rep != null) {
                        IToken[] completionsForModule = getCompletionsForModule(useModule,
                                state.getCopyWithActTok(rep));
                        if (completionsForModule != null && completionsForModule.length > 0) {
                            return completionsForModule;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Unpacks the type of the content of a list and gets completions based on it.
     * @param unpackPos
     */
    private IToken[] getCompletionsFromUnpackedCompoundObject(IModule module, ICompletionState state,
            exprType[] elts, UnpackInfo unpackPos) throws CompletionRecursionException {

        if (elts != null && elts.length > 0) {
            exprType elt = elts[0];
            if (elt instanceof org.python.pydev.parser.jython.ast.Tuple
                    || elt instanceof org.python.pydev.parser.jython.ast.List) {
                if (unpackPos.getUnpackFor()) {
                    elts = NodeUtils.getEltsFromCompoundObject(elt);
                }
            }
            String rep;
            int unpackTuple = unpackPos.getUnpackTuple(elts.length);
            if (unpackTuple >= 0) {
                rep = NodeUtils.getFullRepresentationString(elts[unpackTuple]);
            } else {
                rep = NodeUtils.getFullRepresentationString(elts[0]);

            }
            if (rep != null) {
                ICompletionState copyWithActTok = state.getCopyWithActTok(rep);
                if (elts[0] instanceof Call) {
                    copyWithActTok.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                }
                IToken[] completionsForModule = getCompletionsForModule(module,
                        copyWithActTok);
                if (completionsForModule.length > 0) {
                    return completionsForModule;
                }
            }
        }
        return null;
    }

    private IToken[] decorateWithLocal(IToken[] tokens, ILocalScope localScope, ICompletionState state) {
        if (localScope != null) {
            Collection<IToken> interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
            if (interfaceForLocal != null && interfaceForLocal.size() > 0) {
                IToken[] ret = new IToken[tokens.length + interfaceForLocal.size()];
                Object[] array = interfaceForLocal.toArray();
                System.arraycopy(array, 0, ret, 0, array.length);
                System.arraycopy(tokens, 0, ret, array.length, tokens.length);
                return ret;
            }
        }
        return tokens;
    }

    private IToken[] getAssignCompletions(IModule module, ICompletionState state, boolean lookForArgumentCompletion,
            ILocalScope localScope) throws CompletionRecursionException {
        state.checkMaxTimeForCompletion();
        AssignCompletionInfo assignCompletions = assignAnalysis.getAssignCompletions(this, module, state, localScope);

        boolean useExtensions = assignCompletions.completions.size() == 0;

        if (lookForArgumentCompletion && localScope != null && assignCompletions.completions.size() == 0
                && assignCompletions.defs.length > 0) {
            //Now, if a definition found was available in the same scope we started on, let's add the
            //tokens that are available from that scope.
            for (Definition d : assignCompletions.defs) {
                if (d.module.equals(module) && localScope.equals(d.scope)) {
                    Collection<IToken> interfaceForLocal = localScope.getInterfaceForLocal(state.getActivationToken());
                    assignCompletions.completions.addAll(interfaceForLocal);
                    break;
                }
            }
        }

        if (useExtensions && localScope != null) {
            assignCompletions.completions.addAll(CompletionParticipantsHelper.getCompletionsForTokenWithUndefinedType(
                    state, localScope));
        }

        return assignCompletions.completions.toArray(EMPTY_ITOKEN_ARRAY);
    }

    /**
     * @see ICodeCompletionASTManager#getCompletionsForClassInLocalScope(IModule, ICompletionState, boolean, boolean, List, HashSet)
     */
    public void getCompletionsForClassInLocalScope(IModule module, ICompletionState state, boolean searchSameLevelMods,
            boolean lookForArgumentCompletion, List<String> lookForClass, HashSet<IToken> hashSet)
                    throws CompletionRecursionException {
        IToken[] tokens;
        //if found here, it's an instanced variable (force it and restore if we didn't find it here...)
        ICompletionState stateCopy = state.getCopy();
        int prevLookingFor = stateCopy.getLookingFor();
        //force looking for instance
        stateCopy.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE, true);

        for (String classFound : lookForClass) {
            stateCopy.setLocalImportsGotten(false);
            stateCopy.setActivationToken(classFound);

            //same thing as the initial request, but with the class we could find...
            tokens = getCompletionsForModule(module, stateCopy, searchSameLevelMods, lookForArgumentCompletion);
            if (tokens != null) {
                for (IToken tok : tokens) {
                    hashSet.add(tok);
                }
            }
        }
        if (hashSet.size() == 0) {
            //force looking for what was set before...
            stateCopy.setLookingFor(prevLookingFor, true);
        }
    }

    /**
     * Attempt to search on modules on the same level as this one (this will only happen if we are in an __init__
     * module (otherwise, the initial set will be empty)
     *
     * @param initial this is the set of tokens generated from modules in the same level
     * @param state the current state of the completion
     *
     * @return a list of tokens found.
     * @throws CompletionRecursionException
     */
    protected IToken[] searchOnSameLevelMods(Set<IToken> initial, ICompletionState state)
            throws CompletionRecursionException {
        IToken[] ret = null;
        Tuple<IModule, IModulesManager> modUsed = null;
        String actTokUsed = null;

        for (IToken token : initial) {
            //ok, maybe it was from the set that is in the same level as this one (this will only happen if we are on an __init__ module)
            String rep = token.getRepresentation();

            if (state.getActivationToken().startsWith(rep)) {
                String absoluteImport = token.getAsAbsoluteImport();
                modUsed = modulesManager.getModuleAndRelatedModulesManager(absoluteImport, state.getNature(), true,
                        false);

                IModule sameLevelMod = null;
                if (modUsed != null) {
                    sameLevelMod = modUsed.o1;
                }

                if (sameLevelMod == null) {
                    return null;
                }

                String qualifier = state.getActivationToken().substring(rep.length());

                if (state.getActivationToken().equals(rep)) {
                    actTokUsed = "";
                } else if (qualifier.startsWith(".")) {
                    actTokUsed = qualifier.substring(1);
                }

                if (actTokUsed != null) {
                    ICompletionState copy = state.getCopyWithActTok(actTokUsed);
                    copy.setBuiltinsGotten(true); //we don't want builtins...
                    ret = getCompletionsForModule(sameLevelMod, copy);
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * @see ICodeCompletionASTManager#getGlobalCompletions
     */
    public List<IToken> getGlobalCompletions(IToken[] globalTokens, IToken[] importedModules,
            IToken[] wildImportedModules, ICompletionState state, IModule current) {
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            log("getGlobalCompletions", current, state);
        }
        List<IToken> completions = new ArrayList<IToken>();

        //in completion with nothing, just go for what is imported and global tokens.
        for (int i = 0; i < globalTokens.length; i++) {
            completions.add(globalTokens[i]);
        }

        //now go for the token imports
        for (int i = 0; i < importedModules.length; i++) {
            completions.add(importedModules[i]);
        }

        if (!state.getBuiltinsGotten()) {
            state.setBuiltinsGotten(true);
            if (DebugSettings.DEBUG_CODE_COMPLETION) {
                Log.toLogFile(this, "getBuiltinCompletions");
            }
            //last thing: get completions from module __builtin__
            getBuiltinCompletions(state, completions);
            if (DebugSettings.DEBUG_CODE_COMPLETION) {
                Log.toLogFile(this, "END getBuiltinCompletions");
            }
        }

        //wild imports: recursively go and get those completions. Must be done before getting the builtins, because
        //when we do a wild import, we may get tokens that are filtered, and there's a chance that the builtins get
        //filtered out if they are gotten from a wild import and not from the module itself.
        for (int i = 0; i < wildImportedModules.length; i++) {

            //for wild imports, we must get the global completions with __all__ filtered
            IToken name = wildImportedModules[i];
            getCompletionsForWildImport(state, current, completions, name);
        }
        return completions;
    }

    /**
     * @return the builtin completions
     */
    public List<IToken> getBuiltinCompletions(ICompletionState state, List<IToken> completions) {
        IPythonNature nature = state.getNature();
        IToken[] builtinCompletions = getBuiltinComps(nature);
        if (builtinCompletions != null) {
            for (int i = 0; i < builtinCompletions.length; i++) {
                completions.add(builtinCompletions[i]);
            }
        }
        return completions;

    }

    /**
     * @return the tokens in the builtins
     */
    protected IToken[] getBuiltinComps(IPythonNature nature) {
        return nature.getBuiltinCompletions();
    }

    /**
     * @return the module that represents the builtins
     */
    protected IModule getBuiltinMod(IPythonNature nature) {
        return nature.getBuiltinMod();
    }

    /**
     * Resolves a token defined with 'from module import something' statement
     * to a proper type, as defined in module.
     * @param imported the token to resolve.
     * @return the resolved token or the original token in case no additional information could be obtained.
     * @throws CompletionRecursionException
     */
    public ImmutableTuple<IModule, IToken> resolveImport(ICompletionState state, final IToken imported, IModule current)
            throws CompletionRecursionException {
        String currModName = imported.getParentPackage();
        Tuple3<IModule, String, IToken> modTok = findOnImportedMods(new IToken[] { imported },
                state.getCopyWithActTok(imported.getRepresentation()), currModName, current);
        if (modTok != null && modTok.o1 != null) {

            if (modTok.o2.length() == 0) {
                return new ImmutableTuple<IModule, IToken>(current, imported); //it's a module actually, so, no problems...

            } else {
                try {
                    state.checkResolveImportMemory(modTok.o1, modTok.o2);
                } catch (CompletionRecursionException e) {
                    return new ImmutableTuple<IModule, IToken>(current, imported);
                }
                IToken repInModule = getRepInModule(modTok.o1, modTok.o2, state.getNature(), state);
                if (repInModule != null) {
                    return new ImmutableTuple<IModule, IToken>(modTok.o1, repInModule);
                }
            }
        }
        return new ImmutableTuple<IModule, IToken>(current, imported);

    }

    /**
     * This is the public interface
     * @throws CompletionRecursionException
     * @see org.python.pydev.core.ICodeCompletionASTManager#getRepInModule(org.python.pydev.core.IModule, java.lang.String, org.python.pydev.core.IPythonNature)
     */
    public IToken getRepInModule(IModule module, String tokName, IPythonNature nature)
            throws CompletionRecursionException {
        return getRepInModule(module, tokName, nature, null);
    }

    /**
     * Get the actual token representing the tokName in the passed module
     * @param module the module where we're looking
     * @param tokName the name of the token we're looking for
     * @param nature the nature we're looking for
     * @return the actual token in the module (or null if it was not possible to find it).
     * @throws CompletionRecursionException
     */
    private IToken getRepInModule(IModule module, String tokName, IPythonNature nature, ICompletionState state)
            throws CompletionRecursionException {
        if (module != null) {
            if (tokName.startsWith(".")) {
                tokName = tokName.substring(1);
            }

            //ok, we are getting some token from the module... let's see if it is really available.
            String[] headAndTail = FullRepIterable.headAndTail(tokName);
            String actToken = headAndTail[0]; //tail (if os.path, it is os)
            String hasToBeFound = headAndTail[1]; //head (it is path)

            //if it was os.path:
            //initial would be os.path
            //foundAs would be os
            //actToken would be path

            //now, what we will do is try to do a code completion in os and see if path is found
            if (state == null) {
                state = CompletionStateFactory.getEmptyCompletionState(actToken, nature, new CompletionCache());
            } else {
                state = state.getCopy();
                state.setActivationToken(actToken);
            }
            IToken[] completionsForModule = getCompletionsForModule(module, state);
            int len = completionsForModule.length;
            for (int i = 0; i < len; i++) {
                IToken foundTok = completionsForModule[i];
                if (foundTok.getRepresentation().equals(hasToBeFound)) {
                    return foundTok;
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see ICodeCompletionASTManager#getCompletionsForWildImport(ICompletionState, IModule, List, IToken)
     */
    public boolean getCompletionsForWildImport(ICompletionState state, IModule current, List<IToken> completions,
            IToken name) {
        try {
            //this one is an exception... even though we are getting the name as a relative import, we say it
            //is not because we want to get the module considering __init__
            IModule mod = null;
            boolean hasImportLevel = false;

            if (name instanceof SourceToken && name.isWildImport()) {
                SourceToken sourceToken = (SourceToken) name;
                SimpleNode ast = sourceToken.getAst();
                if (ast instanceof ImportFrom) {
                    ImportFrom importFrom = (ImportFrom) ast;
                    if (importFrom.level > 0) {
                        hasImportLevel = true;
                        //Ok, we have a relative import for sure, so, let's check it...
                        String currentName = current.getName();
                        int currLevel = importFrom.level;
                        while (currLevel > 0) {
                            currLevel--;
                            int i = currentName.lastIndexOf('.');
                            if (i == -1) {
                                break;
                            }
                            currentName = currentName.substring(0, i);
                        }
                        if (currentName.length() > 0) {
                            currentName += "." + sourceToken.getOriginalRep();
                        } else {
                            currentName = sourceToken.getOriginalRep();
                        }
                        mod = getModule(currentName, state.getNature(), false);
                    }
                }
            }

            if (!hasImportLevel && mod == null) {
                //I.e.: if it has an import level, we can't find it this way...
                if (current != null) {
                    //we cannot get the relative path if we don't have a current module
                    mod = getModule(name.getAsRelativeImport(current.getName()), state.getNature(), false);
                }

                if (mod == null) {
                    mod = getModule(name.getOriginalRep(), state.getNature(), false); //absolute import
                }
            }

            if (mod != null) {
                state.checkWildImportInMemory(current, mod);
                IToken[] completionsForModule = getCompletionsForModule(mod, state, true, false, true);
                for (IToken token : completionsForModule) {
                    completions.add(token);
                }
                return true;
            } else {
                //"Module not found:" + name.getRepresentation()
            }
        } catch (CompletionRecursionException e) {
            //probably found a recursion... let's return the tokens we have so far
        }
        return false;
    }

    public IToken[] findTokensOnImportedMods(IToken[] importedModules, ICompletionState state, IModule current)
            throws CompletionRecursionException {
        Tuple3<IModule, String, IToken> o = findOnImportedMods(importedModules, state, current.getName(), current);

        if (o == null) {
            return null;
        }

        IModule mod = o.o1;
        String tok = o.o2;
        String tokForSearchInOtherModule = getTokToSearchInOtherModule(o);

        if (tok.length() == 0) {
            //the activation token corresponds to an imported module. We have to get its global tokens and return them.
            ICompletionState copy = state.getCopy();
            copy.setActivationToken("");
            copy.setBuiltinsGotten(true); //we don't want builtins...
            return getCompletionsForModule(mod, copy);
        } else if (mod != null) {
            ICompletionState copy = state.getCopy();
            copy.setActivationToken(tokForSearchInOtherModule);
            copy.setCol(-1);
            copy.setLine(-1);
            copy.raiseNFindTokensOnImportedModsCalled(mod, tokForSearchInOtherModule);

            String parentPackage = o.o3.getParentPackage();
            if (parentPackage.trim().length() > 0 && parentPackage.equals(current.getName())
                    && state.getActivationToken().equals(tok) && !parentPackage.endsWith("__init__")) {
                String name = mod.getName();
                if (name.endsWith(".__init__")) {
                    name = name.substring(0, name.length() - 9);
                }
                if (o.o3.getAsAbsoluteImport().startsWith(name)) {
                    if (current.isInDirectGlobalTokens(tok, state)) {
                        return null;
                    }
                }
            }

            return getCompletionsForModule(mod, copy);
        }
        return null;
    }

    /**
     * When we have an import, we have one token which we used to find it and another which is the
     * one we refer to at the current module. This method will get the way it's referred at the
     * actual module and not at the current module (at the current module it's modTok.o2).
     */
    public static String getTokToSearchInOtherModule(Tuple3<IModule, String, IToken> modTok) {
        String tok = modTok.o2;
        String tokForSearchInOtherModule = tok;

        if (tok.length() > 0) {
            IToken sourceToken = modTok.o3;
            if (sourceToken instanceof SourceToken) {
                SourceToken sourceToken2 = (SourceToken) sourceToken;
                if (sourceToken2.getAst() instanceof ImportFrom) {
                    ImportFrom importFrom = (ImportFrom) sourceToken2.getAst();
                    if (importFrom.names.length > 0 && importFrom.names[0].asname != null) {
                        String originalRep = sourceToken.getOriginalRep();
                        tokForSearchInOtherModule = FullRepIterable.getLastPart(originalRep);
                    }
                }
            }
        }
        return tokForSearchInOtherModule;
    }

    /**
     * @param activationToken
     * @param importedModules
     * @param module
     * @return tuple with:
     * 0: mod
     * 1: tok
     * @throws CompletionRecursionException
     */
    public Tuple3<IModule, String, IToken> findOnImportedMods(ICompletionState state, IModule current)
            throws CompletionRecursionException {
        IToken[] importedModules = current.getTokenImportedModules();
        return findOnImportedMods(importedModules, state, current.getName(), current);
    }

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
    public Tuple3<IModule, String, IToken> findOnImportedMods(IToken[] importedModules, ICompletionState state,
            String currentModuleName, IModule current) throws CompletionRecursionException {

        FullRepIterable iterable = new FullRepIterable(state.getActivationToken(), true);
        for (String tok : iterable) {
            for (IToken importedModule : importedModules) {

                final String modRep = importedModule.getRepresentation(); //this is its 'real' representation (alias) on the file (if it is from xxx import a as yyy, it is yyy)

                if (modRep.equals(tok)) {
                    String act = state.getActivationToken();
                    Tuple<IModule, String> r;
                    try {
                        r = findOnImportedMods(importedModule, tok, state, act, currentModuleName, current);
                        if (r != null) {
                            return new Tuple3<IModule, String, IToken>(r.o1, r.o2, importedModule);
                        }
                        //Note, if r==null, even though the name matched, keep on going (to handle cases of
                        //try..except ImportError, as we cannot be sure of which version will actually match).
                    } catch (MisconfigurationException e) {
                        Log.log(e);
                    }
                }
            }
        }
        return null;
    }

    public Tuple<IModule, String> findModule(String moduleToFind, String currentModule, ICompletionState state,
            IModule current) throws CompletionRecursionException, MisconfigurationException {
        NameTok name = new NameTok(moduleToFind, NameTok.ImportModule);
        Import impTok = new Import(new aliasType[] { new aliasType(name, null) });

        List<IToken> tokens = new ArrayList<IToken>();
        List<IToken> imp = AbstractVisitor.makeImportToken(impTok, tokens, currentModule, true);
        IToken importedModule = imp.get(imp.size() - 1); //get the last one (it's the one with the 'longest' representation).
        return this.findOnImportedMods(importedModule, "", state, "", currentModule, current);
    }

    /**
     * Checks if some module can be resolved and returns the module it is resolved to (and to which token).
     * @throws CompletionRecursionException
     * @throws MisconfigurationException
     *
     */
    public Tuple<IModule, String> findOnImportedMods(IToken importedModule, String tok, ICompletionState state,
            String activationToken, String currentModuleName, IModule current) throws CompletionRecursionException,
                    MisconfigurationException {

        Tuple<IModule, String> modTok = null;
        IModule mod = null;

        //ok, check if it is a token for the new import
        IPythonNature nature = state.getNature();
        if (importedModule instanceof SourceToken) {
            SourceToken token = (SourceToken) importedModule;

            if (token.isImportFrom()) {
                ImportFrom importFrom = (ImportFrom) token.getAst();
                int level = importFrom.level;
                if (level > 0) {
                    //ok, it must be treated as a relative import
                    //ok, it is the import added on python 2.5 (from .. import xxx)

                    String parentPackage = token.getParentPackage();
                    List<String> moduleParts = StringUtils.dotSplit(parentPackage);
                    String relative = null;
                    if (moduleParts.size() > level) {
                        relative = FullRepIterable.joinParts(moduleParts, moduleParts.size() - level);
                    }

                    String modName = ((NameTok) importFrom.module).id;
                    if (modName.length() > 0) {
                        //ok, we have to add the other part too, as we have more than the leading dots
                        //from ..bar import
                        relative += "." + modName;
                    }

                    if (!AbstractVisitor.isWildImport(importFrom)) {
                        tok = FullRepIterable.getLastPart(token.originalRep);
                        relative += "." + tok;
                    }

                    modTok = findModuleFromPath(relative, nature, false, null);
                    mod = modTok.o1;
                    if (checkValidity(currentModuleName, mod)) {
                        Tuple<IModule, String> ret = fixTok(modTok, tok, activationToken);
                        return ret;
                    }
                    //ok, it is 'forced' as relative import because it has a level, so, it MUST return here
                    return null;
                }
            }
        }

        boolean isAbsoluteImportEnabledx = this.isAbsoluteImportEnabled(current, nature);

        String asRelativeImport = "";
        if (!isAbsoluteImportEnabledx) {
            //check as relative with complete rep
            asRelativeImport = importedModule.getAsRelativeImport(currentModuleName);
            if (!asRelativeImport.startsWith(".")) {
                modTok = findModuleFromPath(asRelativeImport, nature, true, currentModuleName);
                mod = modTok.o1;
                if (checkValidity(currentModuleName, mod)) {
                    Tuple<IModule, String> ret = fixTok(modTok, tok, activationToken);
                    return ret;
                }
            }
        }

        //check if the import actually represents some token in an __init__ file
        String originalWithoutRep = importedModule.getOriginalWithoutRep();
        if (originalWithoutRep.length() > 0) {
            if (!originalWithoutRep.endsWith("__init__")) {
                originalWithoutRep = originalWithoutRep + ".__init__";
            }
            modTok = findModuleFromPath(originalWithoutRep, nature, true, null);
            mod = modTok.o1;
            if (modTok.o2.endsWith("__init__") == false && checkValidity(currentModuleName, mod)) {
                if (mod.isInGlobalTokens(importedModule.getRepresentation(), nature, false, state)) {
                    //then this is the token we're looking for (otherwise, it might be a module).
                    Tuple<IModule, String> ret = fixTok(modTok, tok, activationToken);
                    if (ret.o2.length() == 0) {
                        ret.o2 = importedModule.getRepresentation();
                    } else {
                        ret.o2 = importedModule.getRepresentation() + "." + ret.o2;
                    }
                    return ret;
                }
            }
        }

        //the most 'simple' case: check as absolute with original rep
        modTok = findModuleFromPath(importedModule.getOriginalRep(), nature, false, null);
        mod = modTok.o1;
        if (checkValidity(currentModuleName, mod)) {
            Tuple<IModule, String> ret = fixTok(modTok, tok, activationToken);
            return ret;
        }

        if (!isAbsoluteImportEnabledx) {
            //ok, one last shot, to see a relative looking in folders __init__
            modTok = findModuleFromPath(asRelativeImport, nature, false, null);
            mod = modTok.o1;
            if (checkValidity(currentModuleName, mod, true)) {
                Tuple<IModule, String> ret = fixTok(modTok, tok, activationToken);
                //now let's see if what we did when we found it as a relative import is correct:

                //if we didn't find it in an __init__ module, all should be ok
                if (!mod.getName().endsWith("__init__")) {
                    return ret;
                }
                //otherwise, we have to be more cautious...
                //if the activation token is empty, then it is the module we were looking for
                //if it is not the initial token we were looking for, it is correct
                //if it is in the global tokens of the found module it is correct
                //if none of this situations was found, we probably just found the same token we had when we started (unless I'm mistaken...)
                else if (activationToken.length() == 0 || ret.o2.equals(activationToken) == false
                        || mod.isInGlobalTokens(activationToken, nature, false, state)) {
                    return ret;
                }
            }
        }

        return null;
    }

    protected boolean checkValidity(String currentModuleName, IModule mod) {
        return checkValidity(currentModuleName, mod, false);
    }

    /**
     * @param isRelative: On a relative import we have to check some more conditions...
     */
    protected boolean checkValidity(String currentModuleName, IModule mod, boolean isRelative) {
        if (mod == null) {
            return false;
        }

        String modName = mod.getName();
        if (modName == null) {
            return true;
        }

        //still in the same module
        if (modName.equals(currentModuleName)) {
            return false;
        }

        if (isRelative && currentModuleName != null && modName.endsWith(".__init__")) {
            //we have to check it without the __init__

            //what happens here is that considering the structure:
            //
            // xxx.__init__
            // xxx.mod1
            //
            // we cannot have tokens from the mod1 getting __init__

            String withoutLastPart = FullRepIterable.getWithoutLastPart(modName);
            String currentWithoutLastPart = FullRepIterable.getWithoutLastPart(currentModuleName);
            if (currentWithoutLastPart.equals(withoutLastPart)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fixes the token if we found a module that was just a substring from the initial activation token.
     *
     * This means that if we had testcase.TestCase and found it as TestCase, the token is added with TestCase
     */
    protected Tuple<IModule, String> fixTok(Tuple<IModule, String> modTok, String tok, String activationToken) {
        if (activationToken.length() > tok.length() && activationToken.startsWith(tok)) {
            String toAdd = activationToken.substring(tok.length() + 1);
            if (modTok.o2.length() == 0) {
                modTok.o2 = toAdd;
            } else {
                modTok.o2 += "." + toAdd;
            }
        }
        return modTok;
    }

    /**
     * This function receives a path (rep) and extracts a module from that path.
     * First it tries with the full path, and them removes a part of the final of
     * that path until it finds the module or the path is empty.
     *
     * @param currentModuleName this is the module name (used to check validity for relative imports) -- not used if dontSearchInit is false
     * if this parameter is not null, it means we're looking for a relative import. When checking for relative imports,
     * we should only check the modules that are directly under this project (so, we should not check the whole pythonpath for
     * it, just direct modules)
     *
     * @return tuple with found module and the String removed from the path in
     * order to find the module.
     */
    public Tuple<IModule, String> findModuleFromPath(String rep, IPythonNature nature, boolean dontSearchInit,
            String currentModuleName) {
        String tok = "";
        boolean lookingForRelative = currentModuleName != null;
        IModule mod = getModule(rep, nature, dontSearchInit, lookingForRelative);
        String mRep = rep;
        int index;
        while (mod == null && (index = mRep.lastIndexOf('.')) != -1) {
            tok = mRep.substring(index + 1) + "." + tok;
            mRep = mRep.substring(0, index);
            if (mRep.length() > 0) {
                mod = getModule(mRep, nature, dontSearchInit, lookingForRelative);
            }
        }
        if (tok.endsWith(".")) {
            tok = tok.substring(0, tok.length() - 1); //remove last point if found.
        }

        if (dontSearchInit && currentModuleName != null && mod != null) {
            String parentModule = FullRepIterable.getParentModule(currentModuleName);
            //if we are looking for some relative import token, it can only match if the name found is not less than the parent
            //of the current module because of the way in that relative imports are meant to be written.

            //if it equal, it should not match either, as it was found as the parent module... this can not happen because it must find
            //it with __init__ if it was the parent module
            if (mod.getName().length() <= parentModule.length()) {
                return new Tuple<IModule, String>(null, null);
            }
        }
        return new Tuple<IModule, String>(mod, tok);
    }
}
