/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ConcreteToken;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractJavaClassModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindDefinitionModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.editor.codecompletion.revisited.visitors.StopVisitingException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.cache.Cache;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * The module should have all the information we need for code completion, find definition, and refactoring on a module.
 *
 * Note: A module may be represented by a folder if it has an __init__.py file that represents the module or a python file.
 *
 * Any of those must be a valid python token to be recognized (from the PYTHONPATH).
 *
 * We don't reuse the ModelUtils already created as we still have to transport a lot of logic to it to make it workable, so, the attempt
 * here is to use a thin tier.
 *
 * NOTE: When using it, don't forget to use the superclass abstraction.
 *
 * @author Fabio Zadrozny
 */
public class SourceModule extends AbstractModule implements ISourceModule {

    private static final IToken[] EMPTY_ITOKEN_ARRAY = new IToken[0];

    private static final boolean DEBUG_INTERNAL_GLOBALS_CACHE = false;

    public static boolean TESTING = false;

    /**
     * This is the abstract syntax tree based on the jython parser output.
     */
    private SimpleNode ast;

    /**
     * File that originated the syntax tree.
     */
    private File file;

    /**
     * Is bootstrap?
     */
    private Boolean bootstrap;

    /**
     * Path for this module within the zip file (only used if file is actually a file... otherwise it is null).
     */
    public String zipFilePath;

    /**
     * This is a parse error that was found when parsing the code that generated this module
     */
    public final Throwable parseError;

    @Override
    public String getZipFilePath() {
        return zipFilePath;
    }

    /**
     * This is the time when the file was last modified.
     */
    private long lastModified;

    /**
     * The object may be a SourceToken or a List<SourceToken>
     */
    private HashMap<Integer, TreeMap<String, Object>> tokensCache = new HashMap<Integer, TreeMap<String, Object>>();

    /**
     * Set when the visiting is done (can hold some metadata, such as __all__ token assign)
     */
    private GlobalModelVisitor globalModelVisitorCache = null;

    /**
     *
     * @return the visitor that was used to generate the internal tokens for this module (if any).
     *
     * May be null
     */
    public GlobalModelVisitor getGlobalModelVisitorCache() {
        return globalModelVisitorCache;
    }

    /**
     * @return a reference to all the modules that are imported from this one in the global context as a from xxx import *
     *
     * This modules are treated specially, as we don't care which tokens were imported. When this is requested, the module is prompted for
     * its tokens.
     */
    @Override
    public IToken[] getWildImportedModules() {
        return getTokens(GlobalModelVisitor.WILD_MODULES, null, null);
    }

    /**
     * Searches for the following import tokens:
     *   import xxx
     *   import xxx as ...
     *   from xxx import xxx
     *   from xxx import xxx as ....
     * Note, that imports with wildcards are not collected.
     * @return an array of references to the modules that are imported from this one in the global context.
     */
    @Override
    public IToken[] getTokenImportedModules() {
        return getTokens(GlobalModelVisitor.ALIAS_MODULES, null, null);
    }

    private Boolean hasFutureImportAbsoluteImportDeclared = null;

    public boolean hasFutureImportAbsoluteImportDeclared() {
        if (hasFutureImportAbsoluteImportDeclared == null) {
            hasFutureImportAbsoluteImportDeclared = false;
            IToken[] tokenImportedModules = getTokenImportedModules();
            for (IToken iToken : tokenImportedModules) {
                if ("__future__.absolute_import".equals(iToken.getOriginalRep())) {
                    hasFutureImportAbsoluteImportDeclared = true;
                    break;
                }
            }
        }
        return hasFutureImportAbsoluteImportDeclared;
    }

    /**
     *
     * @return the file this module corresponds to.
     */
    @Override
    public File getFile() {
        return this.file;
    }

    /**
     * @return the tokens that are present in the global scope.
     *
     * The tokens can be class definitions, method definitions and attributes.
     */
    @Override
    public IToken[] getGlobalTokens() {
        return getTokens(GlobalModelVisitor.GLOBAL_TOKENS, null, null);
    }

    /**
     * @return a string representing the module docstring.
     */
    @Override
    public String getDocString() {
        IToken[] l = getTokens(GlobalModelVisitor.MODULE_DOCSTRING, null, null);
        if (l.length > 0) {
            SimpleNode a = ((SourceToken) l[0]).getAst();

            return ((Str) a).s;
        }
        return "";
    }

    /**
     * Checks if it is in the global tokens that were created in this module
     * @param tok the token we are looking for
     * @param nature the nature
     * @return true if it was found and false otherwise
     */
    @Override
    public boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache) {
        TreeMap<String, Object> tokens = tokensCache.get(GlobalModelVisitor.GLOBAL_TOKENS);
        if (tokens == null) {
            getGlobalTokens();
        }

        boolean ret = false;
        if (tokens != null) {
            synchronized (tokens) {
                ret = tokens.containsKey(tok);
            }
        }

        if (ret == false) {
            ret = isInDirectImportTokens(tok);
        }
        return ret;
    }

    public boolean isInDirectImportTokens(String tok) {
        TreeMap<String, Object> tokens = tokensCache.get(GlobalModelVisitor.ALIAS_MODULES);
        if (tokens != null) {
            getTokenImportedModules();
        }

        boolean ret = false;
        if (tokens != null) {
            synchronized (tokens) {
                ret = tokens.containsKey(tok);
            }
        }
        return ret;
    }

    /**
     * @param lookOnlyForNameStartingWith: if not null, well only get from the cache tokens starting with the given representation
     * @return a list of IToken
     */
    @SuppressWarnings("unchecked")
    private synchronized IToken[] getTokens(int which, ICompletionState state, String lookOnlyForNameStartingWith) {
        if ((which & GlobalModelVisitor.INNER_DEFS) != 0) {
            throw new RuntimeException("Cannot do this one with caches");
        }
        //cache
        TreeMap<String, Object> tokens = tokensCache.get(which);

        if (tokens != null) {
            return createArrayFromCacheValues(tokens, lookOnlyForNameStartingWith);
        }
        //end cache

        try {
            tokensCache.put(GlobalModelVisitor.ALIAS_MODULES, new TreeMap<String, Object>());
            tokensCache.put(GlobalModelVisitor.GLOBAL_TOKENS, new TreeMap<String, Object>());
            tokensCache.put(GlobalModelVisitor.WILD_MODULES, new TreeMap<String, Object>());
            tokensCache.put(GlobalModelVisitor.MODULE_DOCSTRING, new TreeMap<String, Object>());

            int all = GlobalModelVisitor.ALIAS_MODULES | GlobalModelVisitor.GLOBAL_TOKENS
                    | GlobalModelVisitor.WILD_MODULES | GlobalModelVisitor.MODULE_DOCSTRING;

            //we request all and put it into the cache (partitioned), because that's faster than making multiple runs through it
            GlobalModelVisitor globalModelVisitor = GlobalModelVisitor.getGlobalModuleVisitorWithTokens(ast, all, name,
                    state, false);

            this.globalModelVisitorCache = globalModelVisitor;

            List<IToken> ret = globalModelVisitor.getTokens();

            if (DEBUG_INTERNAL_GLOBALS_CACHE) {
                System.out.println("\n\nLooking for:" + which);
            }
            //cache
            for (IToken token : ret) {
                int choice;
                if (token.isWildImport()) {
                    choice = GlobalModelVisitor.WILD_MODULES;
                } else if (token.isImportFrom() || token.isImport()) {
                    choice = GlobalModelVisitor.ALIAS_MODULES;
                } else if (token.isString()) {
                    choice = GlobalModelVisitor.MODULE_DOCSTRING;
                } else {
                    choice = GlobalModelVisitor.GLOBAL_TOKENS;
                }
                String rep = token.getRepresentation();
                if (DEBUG_INTERNAL_GLOBALS_CACHE) {
                    System.out.println("Adding choice:" + choice + " name:" + rep);
                    if (choice != which) {
                        System.out.println("Looking for:" + which + "found:" + choice);
                        System.out.println("here");
                    }
                }
                TreeMap<String, Object> treeMap = tokensCache.get(choice);
                SourceToken newSourceToken = (SourceToken) token;
                Object current = treeMap.get(rep);
                if (current == null) {
                    treeMap.put(rep, newSourceToken);
                } else {
                    //the new references (later in the module) are always added to the head of the position...
                    if (current instanceof List) {
                        ((List<SourceToken>) current).add(0, newSourceToken);

                    } else if (current instanceof SourceToken) {
                        ArrayList<SourceToken> lst = new ArrayList<SourceToken>();
                        lst.add(newSourceToken);
                        lst.add((SourceToken) current);
                        treeMap.put(rep, lst);

                    } else {
                        throw new RuntimeException("Unexpected class in cache:" + current);

                    }
                }
            }
            //end cache

        } catch (Exception e) {
            Log.log(e);
        }

        //now, let's get it from the cache... (which should be filled by now)
        tokens = tokensCache.get(which);
        return createArrayFromCacheValues(tokens, lookOnlyForNameStartingWith);
    }

    @SuppressWarnings("unchecked")
    private IToken[] createArrayFromCacheValues(TreeMap<String, Object> tokens, String lookOnlyForNameStartingWith) {
        List<SourceToken> ret = new ArrayList<SourceToken>();

        Collection<Object> lookIn;
        if (lookOnlyForNameStartingWith == null) {
            lookIn = tokens.values();
        } else {
            lookIn = tokens.subMap(lookOnlyForNameStartingWith, lookOnlyForNameStartingWith + "z").values();
        }

        for (Object o : lookIn) {
            if (o instanceof SourceToken) {
                ret.add((SourceToken) o);
            } else if (o instanceof List) {
                ret.addAll((List<SourceToken>) o);
            } else {
                throw new RuntimeException("Unexpected class in cache:" + o);
            }
        }
        return ret.toArray(new SourceToken[ret.size()]);
    }

    /**
     *
     * @param name
     * @param f
     * @param n
     */
    public SourceModule(String name, File f, SimpleNode n, Throwable parseError) {
        super(name);
        this.ast = n;
        this.file = f;
        this.parseError = parseError;
        if (f != null) {
            this.lastModified = FileUtils.lastModified(f);
        }
    }

    /**
     * @see org.python.pydev.core.IModule#getGlobalTokens(org.python.pydev.core.ICompletionState, org.python.pydev.core.ICodeCompletionASTManager)
     */
    @Override
    public IToken[] getGlobalTokens(ICompletionState initialState, ICodeCompletionASTManager manager) {
        String activationToken = initialState.getActivationToken();
        final int activationTokenLen = activationToken.length();
        final List<String> actToks = StringUtils.dotSplit(activationToken);
        final int actToksLen = actToks.size();

        String goFor = null;
        if (actToksLen > 0) {
            goFor = actToks.get(0);
        }
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS, null, goFor);

        for (int i = 0; i < t.length; i++) {
            SourceToken token = (SourceToken) t[i];
            String rep = token.getRepresentation();

            SimpleNode ast = token.getAst();

            if (activationTokenLen > rep.length() && activationToken.startsWith(rep)) {
                //we need this thing to work correctly for nested modules...
                //some tests are available at: PythonCompletionTestWithoutBuiltins.testDeepNestedXXX

                int iActTok = 0;
                if (actToks.get(iActTok).equals(rep)) {
                    //System.out.println("Now we have to find act..."+activationToken+"(which is a definition of:"+rep+")");
                    try {
                        Definition[] definitions;
                        String value = activationToken;
                        String initialValue = null;
                        while (true) {
                            if (value.equals(initialValue)) {
                                break;
                            }
                            initialValue = value;
                            if (iActTok > actToksLen) {
                                break; //unable to find it
                            }

                            //If we have C1.f.x
                            //At this point we'll find the C1 definition...

                            definitions = findDefinition(initialState.getCopyWithActTok(value),
                                    token.getLineDefinition(), token.getColDefinition() + 1, manager.getNature());
                            if (definitions.length == 1) {
                                Definition d = definitions[0];
                                if (d.ast instanceof Assign) {
                                    Assign assign = (Assign) d.ast;
                                    value = NodeUtils.getRepresentationString(assign.value);
                                    definitions = findDefinition(initialState.getCopyWithActTok(value), d.line, d.col,
                                            manager.getNature());
                                } else if (d.ast instanceof ClassDef) {
                                    IToken[] toks = ((SourceModule) d.module).getClassToks(initialState,
                                            manager, d.ast).toArray(EMPTY_ITOKEN_ARRAY);
                                    if (iActTok == actToksLen - 1) {
                                        return toks;
                                    }
                                    value = d.value;

                                } else if (d.ast instanceof Name) {
                                    ClassDef classDef = (ClassDef) d.scope.getClassDef();
                                    if (classDef != null) {
                                        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(
                                                actToks.get(actToksLen - 1), d.line, d.col, d.module);
                                        try {
                                            classDef.accept(visitor);
                                        } catch (StopVisitingException e) {
                                            //expected exception
                                        }
                                        if (visitor.definitions.size() == 0) {
                                            return EMPTY_ITOKEN_ARRAY;
                                        }
                                        d = visitor.definitions.get(0);
                                        value = d.value;
                                        if (d instanceof AssignDefinition) {
                                            //Yes, at this point we really are looking for an assign!
                                            //E.g.:
                                            //
                                            //import my.module
                                            //
                                            //class A:
                                            //    objects = my.module.Class()
                                            //
                                            //This happens when completing on A.objects.
                                            initialState.setLookingFor(ICompletionState.LOOKING_FOR_ASSIGN, true);
                                            return getValueCompletions(initialState, manager, value, d.module);
                                        }
                                    } else {
                                        if (d.module instanceof SourceModule) {
                                            SourceModule m = (SourceModule) d.module;
                                            String joined = FullRepIterable.joinFirstParts(actToks);
                                            Definition[] definitions2 = m.findDefinition(
                                                    initialState.getCopyWithActTok(joined), d.line, d.col,
                                                    manager.getNature());
                                            if (definitions2.length == 0) {
                                                return EMPTY_ITOKEN_ARRAY;
                                            }
                                            d = definitions2[0];
                                            value = d.value + "." + actToks.get(actToksLen - 1);
                                            if (d instanceof AssignDefinition) {
                                                return ((SourceModule) d.module).getValueCompletions(initialState,
                                                        manager, value, d.module);
                                            }
                                        }
                                    }

                                } else if ((d.ast == null && d.module != null) || d.ast instanceof ImportFrom) {
                                    return getValueCompletions(initialState, manager, value, d.module);

                                } else {
                                    break;
                                }
                            } else {
                                return getValueCompletions(initialState, manager, value, this);
                            }
                            iActTok++;
                        }
                    } catch (CompletionRecursionException e) {
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            } else if (rep.equals(activationToken)) {
                if (ast instanceof ClassDef) {
                    initialState.setLookingFor(ICompletionState.LOOKING_FOR_UNBOUND_VARIABLE);
                }
                List<IToken> classToks = getClassToks(initialState, manager, ast);
                if (classToks.size() == 0) {
                    if (initialState.getLookingFor() == ICompletionState.LOOKING_FOR_ASSIGN) {
                        continue;
                    }
                    //otherwise, return it empty anyway...
                    return EMPTY_ITOKEN_ARRAY;
                }
                return classToks.toArray(EMPTY_ITOKEN_ARRAY);
            }
        }
        return EMPTY_ITOKEN_ARRAY;
    }

    /**
     * @param initialState
     * @param manager
     * @param value
     * @return
     * @throws CompletionRecursionException
     */
    private IToken[] getValueCompletions(ICompletionState initialState, ICodeCompletionASTManager manager,
            String value, IModule module) throws CompletionRecursionException {
        initialState.checkFindMemory(this, value);
        ICompletionState copy = initialState.getCopy();
        copy.setActivationToken(value);
        IToken[] completionsForModule = manager.getCompletionsForModule(module, copy);
        return completionsForModule;
    }

    /**
     * @param initialState
     * @param manager
     * @param ast
     * @return
     */
    public List<IToken> getClassToks(ICompletionState initialState, ICodeCompletionASTManager manager, SimpleNode ast) {
        List<IToken> modToks = GlobalModelVisitor.getTokens(ast, GlobalModelVisitor.INNER_DEFS, name, initialState,
                false);//name = moduleName

        try {
            //COMPLETION: get the completions for the whole hierarchy if this is a class!!
            ICompletionState state;
            if (ast instanceof ClassDef) {
                ClassDef c = (ClassDef) ast;
                for (int j = 0; j < c.bases.length; j++) {
                    if (c.bases[j] instanceof Name) {
                        Name n = (Name) c.bases[j];
                        String base = n.id;
                        //An error in the programming might result in an error.
                        //
                        //e.g. The case below results in a loop.
                        //
                        //class A(B):
                        //
                        //    def a(self):
                        //        pass
                        //
                        //class B(A):
                        //
                        //    def b(self):
                        //        pass
                        state = initialState.getCopy();
                        state.setActivationToken(base);

                        state.checkMemory(this, base);

                        final IToken[] comps = manager.getCompletionsForModule(this, state);
                        modToks.addAll(Arrays.asList(comps));
                    } else if (c.bases[j] instanceof Attribute) {
                        Attribute attr = (Attribute) c.bases[j];
                        String s = NodeUtils.getFullRepresentationString(attr);

                        state = initialState.getCopy();
                        state.setActivationToken(s);
                        final IToken[] comps = manager.getCompletionsForModule(this, state);
                        modToks.addAll(Arrays.asList(comps));
                    }
                }

            }
        } catch (CompletionRecursionException e) {
            // let's return what we have so far...
        }
        return modToks;
    }

    /**
     * Caches to hold scope visitors.
     */
    private Cache<Object, FindScopeVisitor> scopeVisitorCache = new LRUCache<Object, FindScopeVisitor>(10);
    private Cache<Object, FindDefinitionModelVisitor> findDefinitionVisitorCache = new LRUCache<Object, FindDefinitionModelVisitor>(
            10);

    /**
     * @return a scope visitor that has already passed through the visiting step for the given line/col.
     *
     * @note we don't have to worry about the ast, as it won't change after we create the source module with it.
     */
    private FindScopeVisitor getScopeVisitor(int line, int col) throws Exception {
        Tuple<Integer, Integer> key = new Tuple<Integer, Integer>(line, col);
        FindScopeVisitor scopeVisitor = this.scopeVisitorCache.getObj(key);
        if (scopeVisitor == null) {
            scopeVisitor = new FindScopeVisitor(line, col);
            if (ast != null) {
                ast.accept(scopeVisitor);
            }
            this.scopeVisitorCache.add(key, scopeVisitor);
        }
        return scopeVisitor;
    }

    /**
     * @return a find definition scope visitor that has already found some definition
     */
    private FindDefinitionModelVisitor getFindDefinitionsScopeVisitor(String rep, int line, int col) throws Exception {
        Tuple3<String, Integer, Integer> key = new Tuple3<String, Integer, Integer>(rep, line, col);
        FindDefinitionModelVisitor visitor = this.findDefinitionVisitorCache.getObj(key);
        if (visitor == null) {
            visitor = new FindDefinitionModelVisitor(rep, line, col, this);
            if (ast != null) {
                try {
                    ast.accept(visitor);
                } catch (StopVisitingException e) {
                    //expected exception
                }
            }
            this.findDefinitionVisitorCache.add(key, visitor);
        }
        return visitor;
    }

    /**
     * Used for tests: tests should initialize this attribute and add listeners to it (and when it finishes, it should
     * be set to null again).
     */
    public static CallbackWithListeners<ICompletionState> onFindDefinition;

    @Override
    @SuppressWarnings("rawtypes")
    public Definition[] findDefinition(ICompletionState state, int line, int col, final IPythonNature nature)
            throws Exception {
        return findDefinition(state, line, col, nature, new HashSet());
    }

    /**
     * @param line: starts at 1
     * @param col: starts at 1
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Definition[] findDefinition(ICompletionState state, int line, int col, final IPythonNature nature,
            Set innerFindPaths) throws Exception {
        state.checkMaxTimeForCompletion();

        if (onFindDefinition != null) {
            onFindDefinition.call(state);
        }
        final String actTok = state.getActivationToken();

        Object key = new Tuple3("findDefinition", this.getName(), actTok);
        if (!innerFindPaths.add(key)) {
            // We're already in the middle of this path, i.e.:
            //a = result.find
            //result = a
            //So, we can't go on this way as it'd recurse!
            return new Definition[0];

        }

        if (actTok.length() == 0) {
            //No activation token means the module itself.
            return new Definition[] { new Definition(1, 1, "", null, null, this) };
        }

        //the line passed in starts at 1 and the lines for the visitor start at 0
        ArrayList<Definition> toRet = new ArrayList<Definition>();

        //first thing is finding its scope
        FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);

        //this visitor checks for assigns for the token
        FindDefinitionModelVisitor visitor = getFindDefinitionsScopeVisitor(actTok, line, col);

        List<Definition> defs = visitor.definitions;
        int size = defs.size();
        if (size > 0) {
            //ok, it is an assign, so, let's get it
            for (int i = 0; i < size; i++) {
                Object next = defs.get(i);
                if (next instanceof AssignDefinition) {
                    AssignDefinition element = (AssignDefinition) next;
                    if (element.target.startsWith("self") == false) {
                        if (element.scope.isOuterOrSameScope(scopeVisitor.scope) || element.foundAsGlobal) {
                            toRet.add(element);
                        }
                    } else {
                        toRet.add(element);
                    }
                } else {
                    toRet.add((Definition) next);
                }
            }
            if (toRet.size() > 0) {
                return toRet.toArray(new Definition[0]);
            }
        }

        //now, check for locals
        IToken[] localTokens = scopeVisitor.scope.getAllLocalTokens();
        int len = localTokens.length;
        for (int i = 0; i < len; i++) {
            IToken tok = localTokens[i];

            final String tokenRep = tok.getRepresentation();
            if (tokenRep.equals(actTok)) {
                return new Definition[] { new Definition(tok, scopeVisitor.scope, this, true) };
            } else if (actTok.startsWith(tokenRep + ".") && !actTok.startsWith("self.")) {
                final int tokenRepLen = tokenRep.length();
                //this means we have a declaration in the local scope and we're accessing a part of it
                //e.g.:
                //class B:
                //    def met2(self):
                //        c = C()
                //        c.met1
                state.checkFindLocalDefinedDefinitionMemory(this, tokenRep);
                ICompletionState copyWithActTok = state.getCopyWithActTok(tokenRep);

                Definition[] definitions = this.findDefinition(copyWithActTok, tok.getLineDefinition(),
                        tok.getColDefinition(), nature, innerFindPaths);
                ArrayList<Definition> ret = new ArrayList<Definition>();
                for (Definition definition : definitions) {
                    if (definition.module != null) {
                        if (definition.value.length() == 0) {
                            continue;
                        }
                        String checkFor = definition.value + actTok.substring(tokenRepLen);
                        if (this.equals(definition.module)) {
                            //no point in finding the starting point
                            if (actTok.equals(definition.value)) {
                                continue;
                            }
                            if (checkFor.equals(actTok)) {
                                continue;
                            }
                            if (checkFor.startsWith(actTok + '.')) {
                                //This means that to find some symbol we have a construct such as:
                                //a = a.strip().rjust()
                                //So, if we look for a.strip, and we resolve a as a.strip.rjust, we'll try to find:
                                //a.strip.rjust.strip, in which case we'd recurse.
                                continue;
                            }
                        }

                        //Note: I couldn't really reproduce this case, so, this fix is just a theoretical
                        //workaround. Hopefully sometime someone will provide some code to reproduce this.
                        //see: http://sourceforge.net/tracker/?func=detail&aid=2992629&group_id=85796&atid=577329
                        int dotsFound = StringUtils.count(checkFor, '.');
                        if (dotsFound > 15) {
                            throw new CompletionRecursionException("Trying to go to deep to find definition.\n"
                                    + "We probably started entering a recursion.\n" + "Module: "
                                    + definition.module.getName() + "\n" + "Token: " + checkFor);
                        }

                        Definition[] realDefinitions;
                        if (definition.module instanceof SourceModule) {
                            SourceModule sourceModule = (SourceModule) definition.module;
                            realDefinitions = sourceModule.findDefinition(
                                    state.getCopyWithActTok(checkFor), definition.line, definition.col, nature,
                                    innerFindPaths);

                        } else {
                            realDefinitions = (Definition[]) definition.module.findDefinition(
                                    state.getCopyWithActTok(checkFor), definition.line, definition.col, nature);

                        }
                        for (Definition realDefinition : realDefinitions) {
                            ret.add(realDefinition);
                        }
                    }
                }
                if (ret.size() == 0) {
                    //Well, it seems it's a parameter, so, let's check if we can get the parameter definition to then resolve
                    //the token.
                    ILocalScope scope = scopeVisitor.scope;
                    List<String> possibleClassesForActivationToken = scope
                            .getPossibleClassesForActivationToken(tokenRep);

                    //Above we have: actTok.startsWith(tokenRep + ".")
                    //and we just resolved tokenRep, so, let's check the remainder given type hints.

                    String remainder = actTok.substring(tokenRepLen + 1);

                    if (possibleClassesForActivationToken.size() > 0) {
                        for (String possibleClass : possibleClassesForActivationToken) {
                            AbstractASTManager astManager = (AbstractASTManager) nature.getAstManager();
                            if (astManager != null) {
                                IToken[] completionsFromTypeRepresentation = astManager
                                        .getCompletionsFromTypeRepresentation(
                                                state, Arrays.asList(possibleClass), this);

                                int length = completionsFromTypeRepresentation.length;
                                for (int j = 0; j < length; j++) {
                                    IToken iToken = completionsFromTypeRepresentation[j];
                                    if (remainder.equals(iToken.getRepresentation())) {
                                        String parentPackage = iToken.getParentPackage();
                                        IModule module;
                                        if (this.getName().equals(parentPackage)) {
                                            module = this;
                                        } else {
                                            module = astManager.getModule(parentPackage, nature, true);
                                        }
                                        if (module != null) {
                                            ret.add(new Definition(iToken, null, module));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return ret.toArray(new Definition[ret.size()]);
            }
        }

        //not found... check as local imports
        List<IToken> localImportedModules = scopeVisitor.scope.getLocalImportedModules(line, col, this.name);
        ICodeCompletionASTManager astManager = nature.getAstManager();
        int lenImportedModules = localImportedModules.size();
        for (int i = 0; i < lenImportedModules; i++) {
            IToken tok = localImportedModules.get(i);
            String importRep = tok.getRepresentation();
            if (importRep.equals(actTok) || actTok.startsWith(importRep + ".")) {
                Tuple3<IModule, String, IToken> o = astManager.findOnImportedMods(new IToken[] { tok },
                        state.getCopyWithActTok(actTok), this.getName(), this);
                if (o != null && o.o1 instanceof SourceModule) {
                    ICompletionState copy = state.getCopy();
                    copy.setActivationToken(o.o2);

                    findDefinitionsFromModAndTok(nature, toRet, null, (SourceModule) o.o1, copy);
                }
                if (toRet.size() > 0) {
                    return toRet.toArray(new Definition[0]);
                }
            }
        }

        //ok, not assign nor import, let's check if it is some self (we do not check for only 'self' because that would map to a
        //local (which has already been covered).
        if (actTok.startsWith("self.")) {
            //ok, it is some self, now, that is only valid if we are in some class definition
            ClassDef classDef = (ClassDef) scopeVisitor.scope.getClassDef();
            if (classDef != null) {
                //ok, we are in a class, so, let's get the self completions
                String classRep = NodeUtils.getRepresentationString(classDef);
                if (classRep != null) {
                    IToken[] globalTokens = getGlobalTokens(new CompletionState(line - 1, col - 1, classRep, nature,
                            "",
                            state), //use the old state as the cache
                            astManager);

                    String withoutSelf = actTok.substring(5);
                    for (IToken token : globalTokens) {
                        if (token.getRepresentation().equals(withoutSelf)) {
                            String parentPackage = token.getParentPackage();
                            IModule module = astManager.getModule(parentPackage, nature, true);

                            if (token instanceof SourceToken
                                    && (module != null || this.name == null || this.name.equals(parentPackage))) {
                                if (module == null) {
                                    module = this;
                                }

                                SimpleNode ast2 = ((SourceToken) token).getAst();
                                Tuple<Integer, Integer> def = getLineColForDefinition(ast2);
                                FastStack<SimpleNode> stack = new FastStack<SimpleNode>(5);
                                if (module instanceof SourceModule) {
                                    stack.push(((SourceModule) module).getAst());
                                }
                                stack.push(classDef);
                                ILocalScope scope = new LocalScope(stack);
                                return new Definition[] { new Definition(def.o1, def.o2, token.getRepresentation(),
                                        ast2,
                                        scope, module) };

                            } else {
                                return new Definition[0];
                            }
                        }
                    }
                }
            }
        }

        //ok, it is not an assign, so, let's search the global tokens (and imports)
        String tok = actTok;
        SourceModule mod = this;

        Tuple3<IModule, String, IToken> o = astManager.findOnImportedMods(state.getCopyWithActTok(actTok), this);

        if (o != null) {
            if (o.o1 instanceof SourceModule) {
                mod = (SourceModule) o.o1;
                tok = o.o2;

            } else if (o.o1 instanceof CompiledModule) {
                //ok, we have to check the compiled module
                tok = o.o2;
                if (tok == null || tok.length() == 0) {
                    return new Definition[] { new Definition(1, 1, "", null, null, o.o1) };
                } else {
                    state.checkFindDefinitionMemory(o.o1, tok);
                    return (Definition[]) o.o1.findDefinition(state.getCopyWithActTok(tok), -1, -1, nature);
                }

            } else if (o.o1 instanceof AbstractJavaClassModule) {
                tok = o.o2;
                state.checkFindDefinitionMemory(o.o1, tok);
                return (Definition[]) o.o1.findDefinition(state.getCopyWithActTok(tok), -1, -1, nature);

            } else {
                throw new RuntimeException("Unexpected module found in imports: " + o);
            }
        }

        //mod == this if we are now checking the globals (or maybe not)...heheheh
        ICompletionState copy = state.getCopy();
        copy.setActivationToken(tok);
        try {
            state.checkFindDefinitionMemory(mod, tok);
            findDefinitionsFromModAndTok(nature, toRet, visitor.moduleImported, mod, copy);
        } catch (CompletionRecursionException e) {
            //ignore (will return what we've got so far)
            //            e.printStackTrace();
        }

        return toRet.toArray(new Definition[0]);
    }

    /**
     * Finds the definitions for some module and a token from that module
     * @throws Exception
     */
    private void findDefinitionsFromModAndTok(IPythonNature nature, ArrayList<Definition> toRet, String moduleImported,
            SourceModule mod, ICompletionState state) throws Exception {
        String tok = state.getActivationToken();
        if (tok != null) {
            if (tok.length() > 0) {
                Definition d = mod.findGlobalTokDef(state.getCopyWithActTok(tok), nature);
                if (d != null) {
                    toRet.add(d);

                } else if (moduleImported != null) {
                    //if it was found as some import (and is already stored as a dotted name), we must check for
                    //multiple representations in the absolute form:
                    //as a relative import
                    //as absolute import
                    getModuleDefinition(nature, toRet, mod, moduleImported);
                }

            } else {
                //we found it, but it is an empty tok (which means that what we found is the actual module).
                toRet.add(new Definition(1, 1, "", null, null, mod));
            }
        }
    }

    private IDefinition getModuleDefinition(IPythonNature nature, ArrayList<Definition> toRet, SourceModule mod,
            String moduleImported) {
        String rel = AbstractToken.makeRelative(mod.getName(), moduleImported);
        IModule modFound = nature.getAstManager().getModule(rel, nature, false);
        if (modFound == null) {
            modFound = nature.getAstManager().getModule(moduleImported, nature, false);
        }
        if (modFound != null) {
            //ok, found it
            Definition definition = new Definition(1, 1, "", null, null, modFound);
            if (toRet != null) {
                toRet.add(definition);
            }
            return definition;
        }
        return null;
    }

    /**
     * @param tok
     * @param nature
     * @return
     * @throws Exception
     */
    public Definition findGlobalTokDef(ICompletionState state, IPythonNature nature) throws Exception {
        String tok = state.getActivationToken();
        String[] headAndTail = FullRepIterable.headAndTail(tok);
        String firstPart = headAndTail[0];
        String rep = headAndTail[1];

        IToken[] tokens = null;
        if (nature != null) {
            tokens = nature.getAstManager().getCompletionsForModule(this, state.getCopyWithActTok(firstPart), true);
        } else {
            tokens = getGlobalTokens();
        }
        for (IToken token : tokens) {
            boolean sameRep = token.getRepresentation().equals(rep);
            if (sameRep) {
                if (token instanceof SourceToken) {
                    if (((SourceToken) token).getType() == IToken.TYPE_OBJECT_FOUND_INTERFACE) {
                        //just having it extracted from the interface from an object does not mean
                        //that it's actual definition was found
                        continue;
                    }
                    //ok, we found it
                    SimpleNode a = ((SourceToken) token).getAst();
                    if (a == null) {
                        continue;
                    }
                    Tuple<Integer, Integer> def = getLineColForDefinition(a);

                    String parentPackage = token.getParentPackage();
                    IModule module = this;
                    if (nature != null) {
                        IModule mod = nature.getAstManager().getModule(parentPackage, nature, true);
                        if (mod != null) {
                            module = mod;
                        }
                    }

                    if (module instanceof SourceModule) {
                        //this is just to get its scope...
                        SourceModule m = (SourceModule) module;
                        FindScopeVisitor scopeVisitor = m.getScopeVisitor(a.beginLine, a.beginColumn);
                        return new Definition(def.o1, def.o2, rep, a, scopeVisitor.scope, module);
                    } else {
                        //line, col
                        return new Definition(def.o1, def.o2, rep, a, new LocalScope(new FastStack<SimpleNode>(5)),
                                module);
                    }
                } else if (token instanceof ConcreteToken) {
                    //a contrete token represents a module
                    String modName = token.getParentPackage();
                    if (modName.length() > 0) {
                        modName += ".";
                    }
                    modName += token.getRepresentation();
                    IModule module = nature.getAstManager().getModule(modName, nature, true);
                    if (module == null) {
                        return null;
                    } else {
                        return new Definition(0 + 1, 0 + 1, "", null, null, module); // it is the module itself
                    }

                } else if (token instanceof CompiledToken) {
                    String parentPackage = token.getParentPackage();
                    FullRepIterable iterable = new FullRepIterable(parentPackage, true);

                    IModule module = null;
                    for (String modName : iterable) {
                        module = nature.getAstManager().getModule(modName, nature, true);
                        if (module != null) {
                            break;
                        }
                    }
                    if (module == null) {
                        return null;
                    }

                    int length = module.getName().length();
                    String finalRep = "";
                    if (parentPackage.length() > length) {
                        finalRep = parentPackage.substring(length + 1) + '.';
                    }
                    finalRep += token.getRepresentation();

                    try {
                        IDefinition[] definitions = module.findDefinition(state.getCopyWithActTok(finalRep), -1, -1,
                                nature);
                        if (definitions.length > 0) {
                            return (Definition) definitions[0];
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Unexpected token:" + token.getClass());
                }
            }
        }

        return null;
    }

    public Tuple<Integer, Integer> getLineColForDefinition(SimpleNode a) {
        int line = a.beginLine;
        int col = a.beginColumn;

        if (a instanceof ClassDef) {
            ClassDef c = (ClassDef) a;
            line = c.name.beginLine;
            col = c.name.beginColumn;

        } else if (a instanceof FunctionDef) {
            FunctionDef c = (FunctionDef) a;
            line = c.name.beginLine;
            col = c.name.beginColumn;
        }

        return new Tuple<Integer, Integer>(line, col);
    }

    /**
     * @param line: at 0
     * @param col: at 0
     */
    @Override
    public IToken[] getLocalTokens(int line, int col, ILocalScope scope) {
        try {
            if (scope == null) {
                FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);
                scope = scopeVisitor.scope;
            }

            return scope.getLocalTokens(line, col, false);
        } catch (Exception e) {
            Log.log(e);
            return EMPTY_ITOKEN_ARRAY;
        }
    }

    /**
     * @param line: at 0
     * @param col: at 0
     */
    @Override
    public ILocalScope getLocalScope(int line, int col) {
        try {
            FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);

            return scopeVisitor.scope;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

    /**
     * @return if the file we have is the same file in the cache.
     */
    public boolean isSynched() {
        if (this.file == null && TESTING) {
            return true; //when testing we can have a source module without a file
        }
        return FileUtils.lastModified(this.file) == this.lastModified;
    }

    public SimpleNode getAst() {
        return ast;
    }

    /**
     * @return the line that ends a given scope (or -1 if not found)
     */
    public int findAstEnd(SimpleNode node) {
        try {
            FindScopeVisitor scopeVisitor = getScopeVisitor(node.beginLine, node.beginColumn);

            return scopeVisitor.scope.getScopeEndLine();
        } catch (Exception e) {
            Log.log(e);
            return -1;
        }
    }

    /**
     * @return the main line (or -1 if not found)
     */
    public int findIfMain() {
        try {
            FindScopeVisitor scopeVisitor = getScopeVisitor(-1, -1);

            return scopeVisitor.scope.getIfMainLine();
        } catch (Exception e) {
            Log.log(e);
            return -1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SourceModule)) {
            return false;
        }
        SourceModule m = (SourceModule) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (zipFilePath == null || m.zipFilePath == null) {
            if (zipFilePath != m.zipFilePath) {
                return false;
            }
            //both null at this point
        } else if (!zipFilePath.equals(m.zipFilePath)) {
            return false;
        }

        if (file == null || m.file == null) {
            if (file != m.file) {
                return false;
            }
            //both null at this point
        } else if (!file.equals(m.file)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 77;
        if (file != null) {
            hash += file.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        if (zipFilePath != null) {
            hash += zipFilePath.hashCode();
        }
        return hash;
    }

    public void setName(String n) {
        this.name = n;
    }

    /**
     * @return true if this is a bootstrap module (i.e.: a module that's only used to load a compiled module with the
     * same name -- that used in eggs)
     *
     * A bootstrapped module is the way that egg handles pyd files:
     * it'll create a file with the same name of the dll (e.g.:
     *
     * for having a umath.pyd, it'll create a umath.py file with the contents below
     *
     * File for boostrap
     * def __bootstrap__():
     *    global __bootstrap__, __loader__, __file__
     *    import sys, pkg_resources, imp
     *    __file__ = pkg_resources.resource_filename(__name__,'umath.pyd')
     *    del __bootstrap__, __loader__
     *    imp.load_dynamic(__name__,__file__)
     * __bootstrap__()
     *
     */
    public boolean isBootstrapModule() {
        if (bootstrap == null) {
            IToken[] ret = getGlobalTokens();
            if (ret != null && (ret.length == 1 || ret.length == 2 || ret.length == 3) && this.file != null) { //also checking 2 or 3 tokens because of __file__ and __name__
                for (int i = 0; i < ret.length; i++) {
                    IToken tok = ret[i];
                    if ("__bootstrap__".equals(tok.getRepresentation())) {
                        //if we get here, we already know that it defined a __bootstrap__, so, let's see if it was also called
                        SimpleNode ast = this.getAst();
                        if (ast instanceof Module) {
                            Module module = (Module) ast;
                            if (module.body != null && module.body.length > 0) {
                                ast = module.body[module.body.length - 1];
                                if (ast instanceof Expr) {
                                    Expr expr = (Expr) ast;
                                    ast = expr.value;
                                    if (ast instanceof Call) {
                                        Call call = (Call) ast;
                                        String callRep = NodeUtils.getRepresentationString(call);
                                        if (callRep != null && callRep.equals("__bootstrap__")) {
                                            //ok, and now , the last thing is checking if there's a dll with the same name...
                                            final String modName = FullRepIterable.getLastPart(this.getName());

                                            File folder = file.getParentFile();
                                            File[] validBootsrappedDlls = folder.listFiles(new FilenameFilter() {

                                                public boolean accept(File dir, String name) {
                                                    int i = name.lastIndexOf('.');
                                                    if (i > 0) {
                                                        String namePart = name.substring(0, i);
                                                        if (namePart.equals(modName)) {
                                                            String extension = name.substring(i + 1);
                                                            if (extension.length() > 0
                                                                    && FileTypesPreferencesPage
                                                                            .isValidDllExtension(extension)) {
                                                                return true;
                                                            }
                                                        }
                                                    }
                                                    return false;
                                                }
                                            });

                                            if (validBootsrappedDlls != null && validBootsrappedDlls.length > 0) {
                                                bootstrap = Boolean.TRUE;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (bootstrap == null) {
                //if still not set, it's not a bootstrap.
                bootstrap = Boolean.FALSE;
            }
        }

        return bootstrap;
    }

    /**
     * @return
     */
    public ModulesKey getModulesKey() {
        if (zipFilePath != null && zipFilePath.length() > 0) {
            return new ModulesKeyForZip(name, file, zipFilePath, true);
        }
        return new ModulesKey(name, file);
    }

}