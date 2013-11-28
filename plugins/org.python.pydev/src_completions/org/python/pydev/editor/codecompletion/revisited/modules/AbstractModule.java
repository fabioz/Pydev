/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.TupleN;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule implements IModule {

    private static final IToken[] EMPTY_TOKEN_ARRAY = new IToken[0];

    /**
     * May be changed for tests
     */
    public static String MODULE_NAME_WHEN_FILE_IS_UNDEFINED = "";

    /** 
     * @see org.python.pydev.core.IModule#getWildImportedModules()
     */
    public abstract IToken[] getWildImportedModules();

    /** 
     * @see org.python.pydev.core.IModule#getFile()
     */
    public abstract File getFile();

    /** 
     * @see org.python.pydev.core.IModule#getTokenImportedModules()
     */
    public abstract IToken[] getTokenImportedModules();

    /** 
     * @see org.python.pydev.core.IModule#getGlobalTokens()
     */
    public abstract IToken[] getGlobalTokens();

    /**
     * Don't deal with zip files unless specifically specified
     */
    public String getZipFilePath() {
        return null;
    }

    /** 
     * @see org.python.pydev.core.IModule#getLocalTokens(int, int, ILocalScope)
     */
    public IToken[] getLocalTokens(int line, int col, ILocalScope scope) {
        return EMPTY_TOKEN_ARRAY;
    }

    /**
     * Checks if it is in the global tokens that were created in this module
     * @param tok the token we are looking for
     * @return true if it was found and false otherwise
     */
    public abstract boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache);

    /** 
     * @throws CompletionRecursionException 
     * @see org.python.pydev.core.IModule#isInGlobalTokens(java.lang.String, org.python.pydev.plugin.nature.PythonNature)
     */
    public boolean isInGlobalTokens(String tok, IPythonNature nature, ICompletionCache completionCache)
            throws CompletionRecursionException {
        return isInGlobalTokens(tok, nature, true, completionCache);
    }

    /** 
     * @throws CompletionRecursionException 
     * @see org.python.pydev.core.IModule#isInGlobalTokens(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean)
     */
    public boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods,
            ICompletionCache completionCache) throws CompletionRecursionException {
        return isInGlobalTokens(tok, nature, searchSameLevelMods, false, completionCache) != IModule.NOT_FOUND;
    }

    public int isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods,
            boolean ifHasGetAttributeConsiderInTokens, ICompletionCache completionCache)
            throws CompletionRecursionException {

        //it's worth checking it if it is not dotted... (much faster as it's in a map already)
        if (tok.indexOf(".") == -1) {
            if (isInDirectGlobalTokens(tok, completionCache)) {
                return IModule.FOUND_TOKEN;
            }
        }

        String[] headAndTail = FullRepIterable.headAndTail(tok);
        String head = headAndTail[1];
        String generateTokensFor = headAndTail[0];

        Map<String, IToken> cachedTokens = getCachedCompletions(tok, nature, searchSameLevelMods, completionCache,
                generateTokensFor);

        if (cachedTokens.containsKey(head)) {
            return IModule.FOUND_TOKEN;
        }

        if (ifHasGetAttributeConsiderInTokens) {

            IToken token = cachedTokens.get("__getattribute__");
            if (token == null || isTokenFromBuiltins(token)) {
                token = cachedTokens.get("__getattr__");
            }
            if (token != null && !isTokenFromBuiltins(token)) {
                return IModule.FOUND_BECAUSE_OF_GETATTR;
            }

            //Try to determine if the user specified it has a @DynamicAttrs.
            String[] parentsHeadAndTail = FullRepIterable.headAndTail(generateTokensFor);
            cachedTokens = getCachedCompletions(tok, nature, searchSameLevelMods, completionCache,
                    parentsHeadAndTail[0]);
            IToken parentToken = cachedTokens.get(parentsHeadAndTail[1]);
            if (parentToken != null) {
                String docString = parentToken.getDocStr();
                if (docString != null) {
                    if (docString.indexOf("@DynamicAttrs") != -1) {
                        //class that has things dynamically defined.
                        return IModule.FOUND_BECAUSE_OF_GETATTR;
                    }
                }
            }
        }

        //if not found until now, it is not defined
        return IModule.NOT_FOUND;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, IToken> getCachedCompletions(String tok, IPythonNature nature, boolean searchSameLevelMods,
            ICompletionCache completionCache, String generateTokensFor) throws CompletionRecursionException {
        //now, check if it's cached in a way we can use it (we cache it not as raw tokens, but as representation --> token)
        //to help in later searches.
        String name = this.getName();
        Object key = new TupleN("isInGlobalTokens", name != null ? name : "", generateTokensFor, tok,
                searchSameLevelMods);
        Map<String, IToken> cachedTokens = (Map<String, IToken>) completionCache.getObj(key);

        if (cachedTokens == null) {
            cachedTokens = internalGenerateCachedTokens(nature, completionCache, generateTokensFor, searchSameLevelMods);
            completionCache.add(key, cachedTokens);
        }
        return cachedTokens;
    }

    private boolean isTokenFromBuiltins(IToken token) {
        String parentPackage = token.getParentPackage();
        return parentPackage.equals("__builtin__") || parentPackage.startsWith("__builtin__.")
                || parentPackage.equals("builtins") || parentPackage.startsWith("builtins.");
    }

    /**
     * Generates the cached tokens in the needed structure for a 'fast' search given a token representation
     * (creates a map with the name of the token --> token).
     */
    private Map<String, IToken> internalGenerateCachedTokens(IPythonNature nature, ICompletionCache completionCache,
            String activationToken, boolean searchSameLevelMods) throws CompletionRecursionException {

        Map<String, IToken> cachedTokens = new HashMap<String, IToken>();

        //if still not found, we have to get all the tokens, including regular and wild imports
        ICompletionState state = CompletionStateFactory.getEmptyCompletionState(nature, completionCache);
        ICodeCompletionASTManager astManager = nature.getAstManager();
        state.setActivationToken(activationToken);

        //we don't want to gather builtins in this case.
        state.setBuiltinsGotten(true);
        IToken[] globalTokens = astManager.getCompletionsForModule(this, state, searchSameLevelMods);
        for (IToken token : globalTokens) {
            String rep = token.getRepresentation();
            IToken t = cachedTokens.get(rep);
            if (t != null) {
                //only override tokens if it's a getattr that's not defined in the builtin module
                if (rep.equals("__getattribute__") || rep.equals("__getattr__")) {
                    if (!isTokenFromBuiltins(token)) {
                        cachedTokens.put(rep, token);
                    }
                }
            } else {
                cachedTokens.put(rep, token);
            }
        }
        return cachedTokens;
    }

    /**
     * The token we're looking for must be the state activation token
     */
    public abstract Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature)
            throws Exception;

    /** 
     * @see org.python.pydev.core.IModule#getGlobalTokens(org.python.pydev.editor.codecompletion.revisited.CompletionState, org.python.pydev.core.ICodeCompletionASTManager)
     */
    public abstract IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager);

    /** 
     * @see org.python.pydev.core.IModule#getDocString()
     */
    public abstract String getDocString();

    /**
     * Name of the module
     */
    protected String name;

    /** 
     * @see org.python.pydev.core.IModule#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Constructor
     * 
     * @param name - name of the module
     */
    protected AbstractModule(String name) {
        this.name = name;
    }

    /**
     * This method creates a source module from a file.
     * 
     * @return
     * @throws IOException 
     * @throws MisconfigurationException 
     */
    public static AbstractModule createModule(String name, File f, IPythonNature nature, boolean checkForPath)
            throws IOException, MisconfigurationException {
        if (PythonPathHelper.isValidFileMod(f.getName())) {
            if (PythonPathHelper.isValidSourceFile(f.getName())) {
                return createModuleFromDoc(name, f, FileUtilsFileBuffer.getDocFromFile(f), nature, checkForPath);

            } else { //this should be a compiled extension... we have to get completions from the python shell.
                return new CompiledModule(name, nature.getAstManager().getModulesManager());
            }
        }

        //if we are here, return null...
        return null;
    }

    /** 
     * This function creates the module given that you have a document (that will be parsed)
     * @throws MisconfigurationException 
     */
    public static SourceModule createModuleFromDoc(String name, File f, IDocument doc, IGrammarVersionProvider nature,
            boolean checkForPath) throws MisconfigurationException {
        //for doc, we are only interested in python files.

        if (f != null) {
            if (!checkForPath || PythonPathHelper.isValidSourceFile(f.getName())) {
                ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, nature, name,
                        f));
                return new SourceModule(name, f, (SimpleNode) obj.ast, obj.error);
            }
        } else {
            ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, nature, name, f));
            return new SourceModule(name, f, (SimpleNode) obj.ast, obj.error);
        }
        return null;
    }

    /**
     * This function creates a module and resolves the module name (use this function if only the file is available).
     * @throws MisconfigurationException 
     */
    public static IModule createModuleFromDoc(File file, IDocument doc, IPythonNature pythonNature)
            throws MisconfigurationException {
        IModulesManager projModulesManager = pythonNature.getAstManager().getModulesManager();
        String moduleName = null;
        if (file != null) {
            moduleName = projModulesManager.resolveModule(FileUtils.getFileAbsolutePath(file));
        }
        if (moduleName == null) {
            moduleName = MODULE_NAME_WHEN_FILE_IS_UNDEFINED;
        }
        IModule module = createModuleFromDoc(moduleName, file, doc, pythonNature, false);
        return module;
    }

    /**
     * Creates a source file generated only from an ast.
     * @param n the ast root
     * @return the module
     */
    public static IModule createModule(SimpleNode n) {
        return new SourceModule(null, null, n, null);
    }

    /**
     * Creates a source file generated only from an ast.
     * 
     * @param n the ast root
     * @param file the module file
     * @param moduleName the name of the module
     * 
     * @return the module
     */
    public static IModule createModule(SimpleNode n, File file, String moduleName) {
        return new SourceModule(moduleName, file, n, null);
    }

    /**
     * @return an empty module representing the key passed.
     */
    public static AbstractModule createEmptyModule(ModulesKey key) {
        if (key instanceof ModulesKeyForZip) {
            ModulesKeyForZip e = ((ModulesKeyForZip) key);
            return new EmptyModuleForZip(key.name, key.file, e.zipModulePath, e.isFile);

        } else {
            return new EmptyModule(key.name, key.file);
        }
    }

    public ILocalScope getLocalScope(int line, int col) {
        return null;
    }

    /** 
     * @see org.python.pydev.core.IModule#toString()
     */
    @Override
    public String toString() {
        String n2 = this.getClass().getName();
        String n = n2.substring(n2.lastIndexOf('.') + 1);
        return this.getName() + " (" + n + ")";
    }

    /**
     * @return true if the name we have ends with .__init__ (default for packages -- others are modules)
     */
    public boolean isPackage() {
        return this.name != null && this.name.endsWith(".__init__");
    }

    public String getPackageFolderName() {
        return FullRepIterable.getParentModule(this.name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractModule)) {
            return false;
        }
        AbstractModule other = (AbstractModule) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
