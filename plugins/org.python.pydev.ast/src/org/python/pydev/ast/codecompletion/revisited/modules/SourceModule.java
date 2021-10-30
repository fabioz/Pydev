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
package org.python.pydev.ast.codecompletion.revisited.modules;

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

import org.python.pydev.ast.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.ast.codecompletion.revisited.AbstractToken;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.ConcreteToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.codecompletion.revisited.visitors.FindDefinitionModelVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.ast.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.ast.codecompletion.revisited.visitors.StopVisitingException;
import org.python.pydev.ast.codecompletion.revisited.visitors.TypeInfoDefinition;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ICompletionState.LookingFor;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.NoExceptionCloseable;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.TypeInfo;
import org.python.pydev.shared_core.cache.Cache;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

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
    public TokensList getWildImportedModules() {
        return new TokensList(getTokens(GlobalModelVisitor.WILD_MODULES, null, null));
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
    public TokensList getTokenImportedModules() {
        return new TokensList(getTokens(GlobalModelVisitor.ALIAS_MODULES, null, null));
    }

    private Boolean hasFutureImportAbsoluteImportDeclared = null;

    private final IPythonNature nature;

    @Override
    public IPythonNature getNature() {
        return nature;
    }

    @Override
    public boolean hasFutureImportAbsoluteImportDeclared() {
        if (hasFutureImportAbsoluteImportDeclared == null) {
            hasFutureImportAbsoluteImportDeclared = false;
            TokensList tokenImportedModules = getTokenImportedModules();
            for (IterTokenEntry entry : tokenImportedModules) {
                IToken iToken = entry.getToken();
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
    public TokensList getGlobalTokens() {
        return new TokensList(getTokens(GlobalModelVisitor.GLOBAL_TOKENS, null, null));
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
                    state, false, nature);

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
    public SourceModule(String name, File f, SimpleNode n, Throwable parseError, IPythonNature nature) {
        super(name);
        this.ast = n;
        this.file = f;
        this.parseError = parseError;
        if (f != null) {
            this.lastModified = FileUtils.lastModified(f);
        }
        this.nature = nature;
    }

    /**
     * @see org.python.pydev.core.IModule#getGlobalTokens(org.python.pydev.core.ICompletionState, org.python.pydev.core.ICodeCompletionASTManager)
     */
    @Override
    public TokensList getGlobalTokens(ICompletionState initialState, ICodeCompletionASTManager manager) {
        String activationToken = initialState.getActivationToken();
        Tuple3<String, String, SourceModule> key = new Tuple3<>("getGlobalTokens", activationToken, this);
        TokensList curr = (TokensList) initialState.getObj(key);
        if (curr != null) {
            return curr;
        }
        TokensList tokens = internalCalculateGlobalTokens(initialState, manager);
        initialState.add(key, tokens.copy());
        return tokens;
    }

    public TokensList internalCalculateGlobalTokens(ICompletionState initialState, ICodeCompletionASTManager manager) {
        String activationToken = initialState.getActivationToken();
        final int activationTokenLen = activationToken.length();
        final List<String> actToks = StringUtils.dotSplit(activationToken);
        final int actToksLen = actToks.size();

        String goFor = null;
        if (actToksLen > 0) {
            goFor = actToks.get(0);
        }
        IToken[] t = getTokens(GlobalModelVisitor.GLOBAL_TOKENS, null, goFor);
        LookingFor lookingFor = null;

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
                                if (d instanceof TypeInfoDefinition) {
                                    TypeInfoDefinition typeInfoDefinition = (TypeInfoDefinition) d;
                                    List<ITypeInfo> lookForClass = new ArrayList<>();
                                    TypeInfo info = typeInfoDefinition.info;
                                    lookForClass.add(info);
                                    TokensList completionsForClassInLocalScope = manager
                                            .getCompletionsForClassInLocalScope(
                                                    d.module, initialState.getCopyWithActTok(info.getActTok()), true,
                                                    false, lookForClass);
                                    completionsForClassInLocalScope
                                            .setLookingFor(LookingFor.LOOKING_FOR_INSTANCED_VARIABLE);
                                    return completionsForClassInLocalScope;

                                }
                                if (d.ast instanceof Assign) {
                                    Assign assign = (Assign) d.ast;
                                    if (assign.targets.length == 1 && assign.targets[0] instanceof Name) {
                                        ClassDef classDef = (ClassDef) d.scope.getClassDef();
                                        if (NodeUtils.isEnum(classDef)) {
                                            return new TokensList(new IToken[] {
                                                    new ConcreteToken("name", "Enum name", "", "enum",
                                                            ConcreteToken.TYPE_ATTR, manager.getNature()),
                                                    new ConcreteToken("value", "Enum value", "", "enum",
                                                            ConcreteToken.TYPE_ATTR, manager.getNature())
                                            });
                                        }
                                    }

                                    if (assign.value instanceof Call) {
                                        lookingFor = LookingFor.LOOKING_FOR_INSTANCED_VARIABLE;
                                    }
                                    if (assign.type != null) {
                                        value = NodeUtils.getRepresentationString(assign.type);
                                        lookingFor = LookingFor.LOOKING_FOR_INSTANCED_VARIABLE;
                                    } else if (assign.value != null) {
                                        value = NodeUtils.getRepresentationString(assign.value);
                                    }
                                    if (value == null) {
                                        break;
                                    }
                                    definitions = findDefinition(initialState.getCopyWithActTok(value), d.line,
                                            d.col,
                                            manager.getNature());
                                } else if (d.ast instanceof ClassDef) {
                                    TokensList toks = ((SourceModule) d.module).getClassToks(initialState, manager,
                                            (ClassDef) d.ast);
                                    if (lookingFor != null) {
                                        toks.setLookingFor(lookingFor);
                                    }
                                    if (iActTok == actToksLen - 1) {
                                        return toks;
                                    }
                                    value = d.value;

                                } else if (d.ast instanceof Name) {
                                    ClassDef classDef = (ClassDef) d.scope.getClassDef();
                                    if (classDef != null) {
                                        if (NodeUtils.isEnum(classDef)) {
                                            return new TokensList(new IToken[] {
                                                    new ConcreteToken("name", "Enum name", "", "enum",
                                                            ConcreteToken.TYPE_ATTR, manager.getNature()),
                                                    new ConcreteToken("value", "Enum value", "", "enum",
                                                            ConcreteToken.TYPE_ATTR, manager.getNature())
                                            });
                                        }

                                        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(
                                                actToks.get(actToksLen - 1), d.line, d.col, d.module,
                                                initialState.getNature());
                                        try {
                                            classDef.accept(visitor);
                                        } catch (StopVisitingException e) {
                                            //expected exception
                                        }
                                        if (visitor.definitions.size() == 0) {
                                            return new TokensList(EMPTY_ITOKEN_ARRAY);
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
                                            initialState.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_ASSIGN,
                                                    true);
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
                                                return new TokensList(EMPTY_ITOKEN_ARRAY);
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
                TokensList classToks;
                if (ast instanceof ClassDef) {
                    classToks = getClassToks(initialState, manager, (ClassDef) ast);
                    initialState.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_UNBOUND_VARIABLE);
                    classToks.setLookingFor(initialState.getLookingFor());
                } else {
                    classToks = getInnerToks(initialState, manager, ast);
                }

                if (classToks.empty()) {
                    if (initialState.getLookingFor() == ICompletionState.LookingFor.LOOKING_FOR_ASSIGN) {
                        continue;
                    }
                    //otherwise, return it empty anyway...
                    return new TokensList();
                }
                return classToks;
            }
        }
        return new TokensList(EMPTY_ITOKEN_ARRAY);
    }

    /**
     * @param initialState
     * @param manager
     * @param ast
     * @return TokensList
     */
    public TokensList getClassToks(ICompletionState initialState, ICodeCompletionASTManager manager,
            ClassDef classDef) {
        ClassDefTokensExtractor classTokensExtractor = new ClassDefTokensExtractor(classDef, this, initialState);
        return classTokensExtractor.getTokens(manager);
    }

    public TokensList getInnerToks(ICompletionState initialState, ICodeCompletionASTManager manager, SimpleNode ast) {
        String moduleName = name;
        TokensList modToks = new TokensList(
                GlobalModelVisitor.getTokens(ast, GlobalModelVisitor.INNER_DEFS, moduleName, initialState,
                        false, this.nature));
        modToks.setLookingFor(initialState.getLookingFor());
        return modToks;
    }

    /**
     * @param initialState
     * @param manager
     * @param value
     * @return
     * @throws CompletionRecursionException
     */
    private TokensList getValueCompletions(ICompletionState initialState, ICodeCompletionASTManager manager,
            String value, IModule module) throws CompletionRecursionException {
        initialState.checkFindMemory(this, value);
        ICompletionState copy = initialState.getCopy();
        copy.setActivationToken(value);
        TokensList completionsForModule = manager.getCompletionsForModule(module, copy);
        return completionsForModule;
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
            scopeVisitor = new FindScopeVisitor(line, col, nature);
            if (ast != null) {
                ast.accept(scopeVisitor);
            }
            this.scopeVisitorCache.add(key, scopeVisitor);
        }
        return scopeVisitor;
    }

    /**
     * @param nature
     * @return a find definition scope visitor that has already found some definition
     */
    private FindDefinitionModelVisitor getFindDefinitionsScopeVisitor(String rep, int line, int col,
            IPythonNature nature) throws Exception {
        Tuple3<String, Integer, Integer> key = new Tuple3<String, Integer, Integer>(rep, line, col);
        FindDefinitionModelVisitor visitor = this.findDefinitionVisitorCache.getObj(key);
        if (visitor == null) {
            visitor = new FindDefinitionModelVisitor(rep, line, col, this, nature);
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

    /**
     * @param line: starts at 1
     * @param col: starts at 1
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Definition[] findDefinition(ICompletionState state, int line, int col, final IPythonNature nature)
            throws Exception {
        Object key = new TupleN(state.getActivationToken(), line, col, this);
        ICompletionCache completionCache = state;
        Definition[] found = (Definition[]) completionCache.getObj(key);
        if (found != null) {
            return found;
        }
        try (NoExceptionCloseable x = state.pushLookingFor(LookingFor.LOOKING_FOR_INSTANCE_UNDEFINED)) {
            Definition[] ret = findDefinition(state, line, col, nature, new HashSet());
            state.add(key, ret);
            return ret;
        }
    }

    /**
     * @param line: starts at 1
     * @param col: starts at 1
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Definition[] findDefinition(ICompletionState state, int line, int col, final IPythonNature nature,
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

        Object objClassDef = scopeVisitor.scope.getClassDef();
        if (objClassDef instanceof ClassDef) {
            ClassDef classDef = (ClassDef) objClassDef;
            if (actTok.equals("super")) {
                if (classDef.bases != null) {
                    List<Definition> lst = new ArrayList<>(classDef.bases.length);
                    for (exprType expr : classDef.bases) {
                        String repr = NodeUtils.getRepresentationString(expr);
                        if (repr != null) {
                            state = state.getCopyWithActTok(repr);
                            Definition[] defs = findDefinition(state, line, col, nature);
                            if (defs != null && defs.length > 0) {
                                lst.addAll(Arrays.asList(defs));
                            }
                        }
                    }
                    if (lst.size() > 0) {
                        return lst.toArray(new Definition[lst.size()]);
                    }
                }
                // Didn't find anything for super
                return new Definition[0];
            } else if (actTok.startsWith("super()")) {
                if (classDef.bases != null) {
                    List<Definition> lst = new ArrayList<>(classDef.bases.length);
                    for (exprType expr : classDef.bases) {
                        String repr = NodeUtils.getRepresentationString(expr);
                        if (repr != null) {
                            state = state.getCopyWithActTok(actTok.replace("super()", repr));
                            Definition[] defs = findDefinition(state, line, col, nature);
                            if (defs != null && defs.length > 0) {
                                lst.addAll(Arrays.asList(defs));
                            }
                        }
                    }
                    if (lst.size() > 0) {
                        return lst.toArray(new Definition[lst.size()]);
                    }
                }
                // Just keep going (may get completions globally).
            }
        }
        //this visitor checks for assigns for the token
        FindDefinitionModelVisitor visitor = getFindDefinitionsScopeVisitor(actTok, line, col, nature);

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
        TokensList localTokens = scopeVisitor.scope.getAllLocalTokens();
        int len = localTokens.size();
        for (IterTokenEntry entry : localTokens) {
            IToken tok = entry.getToken();

            final String tokenRep = tok.getRepresentation();
            if (tokenRep.equals(actTok)) {
                if (tok instanceof SourceToken && ((SourceToken) tok).getAst() instanceof Assign) {
                    Assign node = (Assign) ((SourceToken) tok).getAst();
                    String target = tok.getRepresentation();
                    return new Definition[] {
                            FindDefinitionModelVisitor.getAssignDefinition(node, target, 0, line, col,
                                    scopeVisitor.scope, this, -1) };
                }
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
                    List<ITypeInfo> possibleClassesForActivationToken = scope
                            .getPossibleClassesForActivationToken(tokenRep);

                    //Above we have: actTok.startsWith(tokenRep + ".")
                    //and we just resolved tokenRep, so, let's check the remainder given type hints.

                    String remainder = actTok.substring(tokenRepLen + 1);

                    if (possibleClassesForActivationToken.size() > 0) {
                        for (ITypeInfo possibleClass : possibleClassesForActivationToken) {
                            AbstractASTManager astManager = (AbstractASTManager) nature.getAstManager();
                            if (astManager != null) {
                                TokensList completionsFromTypeRepresentation = astManager
                                        .getCompletionsFromTypeRepresentation(
                                                state, Arrays.asList(possibleClass), this);

                                for (IterTokenEntry entry1 : completionsFromTypeRepresentation) {
                                    IToken iToken = entry1.getToken();
                                    if (remainder.equals(iToken.getRepresentation())) {
                                        String parentPackage = iToken.getParentPackage();
                                        IModule module;
                                        if (this.getName().equals(parentPackage)) {
                                            module = this;
                                        } else {
                                            module = astManager.getModule(parentPackage, nature, true, state);
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
        TokensList localImportedModules = scopeVisitor.scope.getLocalImportedModules(line, col, this.name);
        ICodeCompletionASTManager astManager = nature.getAstManager();
        for (IterTokenEntry entry : localImportedModules) {
            IToken tok = entry.getToken();
            String importRep = tok.getRepresentation();
            if (importRep.equals(actTok) || actTok.startsWith(importRep + ".")) {
                Tuple3<IModule, String, IToken> o = astManager.findOnImportedMods(new TokensList(new IToken[] { tok }),
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
                    TokensList globalTokens = getGlobalTokens(
                            new CompletionState(line - 1, col - 1, classRep, nature,
                                    "",
                                    state), //use the old state as the cache
                            astManager);

                    String withoutSelf = actTok.substring(5);
                    for (IterTokenEntry entry : globalTokens) {
                        IToken token = entry.getToken();
                        if (token.getRepresentation().equals(withoutSelf)) {
                            String parentPackage = token.getParentPackage();
                            IModule module = astManager.getModule(parentPackage, nature, true, state);

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
                                ILocalScope scope = new LocalScope(astManager.getNature(), stack);
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

            } else if (o.o1 instanceof IAbstractJavaClassModule) {
                tok = o.o2;
                state.checkFindDefinitionMemory(o.o1, tok);
                return (Definition[]) o.o1.findDefinition(state.getCopyWithActTok(tok), -1, -1, nature);

            } else {
                throw new RuntimeException("Unexpected module found in imports: " + o);
            }
        }

        //mod == this if we are now checking the globals (or maybe not)...heheheh
        ICompletionState copy = state.getCopyWithActTok(tok, line - 1, col - 1);
        // further on this we will get the first part of the same tok, so token line and column will be the same
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
                Definition d = mod.findGlobalTokDef(state.getCopyWithActTok(tok, state.getLine(), state.getCol()),
                        nature);
                if (d != null) {
                    toRet.add(d);

                } else if (moduleImported != null) {
                    //if it was found as some import (and is already stored as a dotted name), we must check for
                    //multiple representations in the absolute form:
                    //as a relative import
                    //as absolute import
                    getModuleDefinition(nature, toRet, mod, moduleImported, state);
                }

            } else {
                //we found it, but it is an empty tok (which means that what we found is the actual module).
                toRet.add(new Definition(1, 1, "", null, null, mod));
            }
        }
    }

    private IDefinition getModuleDefinition(IPythonNature nature, ArrayList<Definition> toRet, SourceModule mod,
            String moduleImported, IModuleRequestState moduleRequest) {
        String rel = AbstractToken.makeRelative(mod.getName(), moduleImported);
        IModule modFound = nature.getAstManager().getModule(rel, nature, false, moduleRequest);
        if (modFound == null) {
            modFound = nature.getAstManager().getModule(moduleImported, nature, false, moduleRequest);
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

    private class DefinitionRef {

        private Definition found;

        public DefinitionRef(Definition found) {
            this.found = found; // may be null;
        }

        public Definition get() {
            return this.found;
        }

    }

    private Definition findGlobalTokDef(ICompletionState state, IPythonNature nature) throws Exception {
        Object key = new Tuple3<>("findGlobalTokDef", state.getActivationToken(), this);
        Object obj = state.getObj(key);
        if (obj != null) {
            return ((DefinitionRef) obj).get();
        }
        Definition found = this.internalFindGlobalTokDef(state, nature);
        state.add(key, new DefinitionRef(found));
        return found;
    }

    /**
     * @param tok
     * @param nature
     * @return
     * @throws Exception
     */
    private Definition internalFindGlobalTokDef(ICompletionState state, IPythonNature nature) throws Exception {
        String tok = state.getActivationToken();
        String[] headAndTail = FullRepIterable.headAndTail(tok);
        String firstPart = headAndTail[0];
        String rep = headAndTail[1];

        TokensList tokens = null;

        // Could use commented code...
        // DefinitionAndCompletions assignCompletions = null;
        // AssignAnalysis assignAnalysis = new AssignAnalysis();
        //
        // if (firstPart.length() > 0) {
        //     ICompletionState copy = state.getCopyWithActTok(firstPart);
        //     assignCompletions = assignAnalysis.getAssignCompletions(nature.getAstManager(),
        //             this, copy, null);
        //
        //     tokens = assignCompletions.completions;
        // }
        if (tokens == null || tokens.empty()) {
            if (nature != null) {
                tokens = nature.getAstManager().getCompletionsForModule(this,
                        state.getCopyWithActTok(firstPart, state.getLine(), state.getCol()), true);
            } else {
                tokens = getGlobalTokens();
            }
        }

        for (IterTokenEntry entry : tokens) {
            IToken token = entry.getToken();
            boolean sameRep = token.getRepresentation().equals(rep);
            if (sameRep) {
                if (token instanceof SourceToken) {
                    SourceToken sourceToken = (SourceToken) token;
                    if (sourceToken.getType() == IToken.TYPE_OBJECT_FOUND_INTERFACE) {
                        //just having it extracted from the interface from an object does not mean
                        //that it's actual definition was found
                        continue;
                    }
                    //ok, we found it
                    SimpleNode a = sourceToken.getAst();
                    if (a == null) {
                        continue;
                    }
                    Tuple<Integer, Integer> def = getLineColForDefinition(a);

                    String parentPackage = token.getParentPackage();
                    IModule module = this;
                    if (nature != null) {
                        IModule mod = nature.getAstManager().getModule(parentPackage, nature, true, state);
                        if (mod != null) {
                            module = mod;
                        }
                    }

                    if (module instanceof SourceModule) {
                        //this is just to get its scope...
                        SourceModule m = (SourceModule) module;

                        FindScopeVisitor scopeVisitor = m.getScopeVisitor(a.beginLine, a.beginColumn);
                        Assign foundInAssign = sourceToken.getFoundInAssign();
                        if (foundInAssign != null) {
                            Definition ret = findDefinitionsInAssignStatementUncached(nature, rep, m,
                                    foundInAssign);
                            if (ret == null) {
                                String fullRep = NodeUtils.getFullRepresentationString(sourceToken.getAst());
                                if (!rep.equals(fullRep)) {
                                    ret = findDefinitionsInAssignStatementUncached(nature, fullRep, m,
                                            foundInAssign);
                                }
                            }
                            if (ret != null) {
                                // Note: we must fix the scope because we visited only the assign in this case (so
                                // it's not correct).
                                ret.scope = scopeVisitor.scope;
                            }

                            // int unpackPos = -1;
                            // int targetPos = 0;
                            //
                            // exprType nodeValue = foundInAssign.value;
                            // String value = NodeUtils.getFullRepresentationString(nodeValue);
                            // if (value == null) {
                            //     value = "";
                            // }
                            //
                            // AssignDefinition newRet = new AssignDefinition(value, rep, targetPos, foundInAssign, def.o1,
                            //         def.o2,
                            //         scopeVisitor.scope, module, nodeValue, unpackPos);
                            return ret;

                        } else {
                            return new Definition(def.o1, def.o2, rep, a, scopeVisitor.scope, module);
                        }
                    } else {
                        //line, col
                        return new Definition(def.o1, def.o2, rep, a,
                                new LocalScope(nature, new FastStack<SimpleNode>(5)),
                                module);
                    }
                } else if (token instanceof ConcreteToken) {
                    //a contrete token represents a module
                    String modName = token.getParentPackage();
                    if (modName.length() > 0) {
                        modName += ".";
                    }
                    modName += token.getRepresentation();
                    IModule module = nature.getAstManager().getModule(modName, nature, true, state);
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
                        module = nature.getAstManager().getModule(modName, nature, true, state);
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
                            Definition definition = (Definition) definitions[0];
                            definition.setGeneratorType(token.getGeneratorType());
                            return definition;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Unexpected token:" + token.getClass());
                }
            }
        }

        // Unable to find the definition... if we previously had a definition, let's try to get typing info from it.
        // if (assignCompletions != null && !rep.contains(".") && !rep.isEmpty()) {
        //     Definition[] parentDefinition = assignCompletions.defs;
        //     for (Definition definition : parentDefinition) {
        //         if (definition instanceof AssignDefinition) {
        //             ArrayList<IDefinition> found = new ArrayList<>();
        //             // Pointing to some other place... let's follow it.
        //             PyRefactoringFindDefinition.findActualDefinition(null, definition.module,
        //                     definition.value, found, definition.line, definition.col,
        //                     state.getNature(), state.getCopyWithActTok(definition.value));
        //             for (IDefinition f : found) {
        //                 if (f instanceof Definition) {
        //                     Definition d = (Definition) f;
        //                     if (d.ast instanceof ClassDef) {
        //                         TypeInfo info = NodeUtils.getTypeForClassDefAttribute(
        //                                 rep, (ClassDef) d.ast);
        //                         if (info != null) {
        //                             return new TypeInfoDefinition(d, d.module, info);
        //                         }
        //                         if (info == null && d.module != null) {
        //                             IModule pyiStubModule = nature.getAstManager().getPyiStubModule(d.module,
        //                                     state);
        //                             if (pyiStubModule instanceof SourceModule) {
        //                                 SourceModule sourceModule = (SourceModule) pyiStubModule;
        //                                 // SimpleNode nodeFromPath = NodeUtils.getNodeFromPath(sourceModule.getAst(),
        //                                 // scopeStackPathNames);
        //                             }
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        return null;
    }

    private Definition findDefinitionsInAssignStatementUncached(IPythonNature nature, String rep, SourceModule m,
            Assign foundInAssign) throws Exception {
        // Note: don't use cache because we'll visit just the assign.
        FindDefinitionModelVisitor visitor = new FindDefinitionModelVisitor(rep,
                foundInAssign.beginLine, foundInAssign.beginColumn, m, nature);
        try {
            foundInAssign.accept(visitor);
        } catch (StopVisitingException e) {
            //expected exception
        }
        List<Definition> definitions = visitor.definitions;
        if (definitions == null || definitions.size() == 0) {
            return null;
        }
        return definitions.get(0);
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
    public TokensList getLocalTokens(int line, int col, ILocalScope scope) {
        try {
            if (scope == null) {
                FindScopeVisitor scopeVisitor = getScopeVisitor(line, col);
                scope = scopeVisitor.scope;
            }

            return scope.getLocalTokens(line, col, false);
        } catch (Exception e) {
            Log.log(e);
            return new TokensList();
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

    @Override
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
            TokensList ret = getGlobalTokens();
            if (ret != null && (ret.size() == 1 || ret.size() == 2 || ret.size() == 3) && this.file != null) { //also checking 2 or 3 tokens because of __file__ and __name__
                for (IterTokenEntry entry : ret) {
                    IToken tok = entry.getToken();
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

                                                @Override
                                                public boolean accept(File dir, String name) {
                                                    int i = name.lastIndexOf('.');
                                                    if (i > 0) {
                                                        String namePart = name.substring(0, i);
                                                        if (namePart.equals(modName)) {
                                                            String extension = name.substring(i + 1);
                                                            if (extension.length() > 0
                                                                    && FileTypesPreferences
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
