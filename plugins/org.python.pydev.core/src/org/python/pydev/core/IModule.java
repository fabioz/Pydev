/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;

import org.python.pydev.core.structure.CompletionRecursionException;

public interface IModule {

    /**
     * @return tokens for the wild imports.
     */
    public abstract IToken[] getWildImportedModules();

    /**
     * @return the file correspondent to this module (may be null if we are unable to get it).
     * Also, this may return a file that is not a source file (such as a .pyc or .pyd).
     */
    public abstract File getFile();

    /**
     * @return the zip file path for this module within the zip file. Should be specified (not null) only if
     * we're actually dealing with a zip file.
     */
    public abstract String getZipFilePath();

    /**
     * @return tokens for the imports in the format from xxx import yyy
     * or import xxx 
     */
    public abstract IToken[] getTokenImportedModules();

    /**
     * This function should get all that is present in the file as global tokens.
     * Note that imports should not be treated by this function (imports have their own functions).
     * 
     * @return
     */
    public abstract IToken[] getGlobalTokens();

    /**
     * This function returns the local completions 
     * @param line starts at 0
     * @param col starts at 0
     * @param localScope the local scope that was previously gotten (if null, it will be created)
     * @return
     */
    public abstract IToken[] getLocalTokens(int line, int col, ILocalScope localScope);

    public abstract boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache);

    public abstract boolean isInGlobalTokens(String tok, IPythonNature nature, ICompletionCache completionCache)
            throws CompletionRecursionException;

    /**
     * @param tok the token we are looking for
     * @param completionCache cache for holding the info requested during a find tokens operation (it may have been
     *      already used in another operation, if it was part of another major operation)
     * @return whether the passed token is part of the global tokens of this module (including imported tokens).
     * @throws CompletionRecursionException 
     */
    public abstract boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods,
            ICompletionCache completionCache) throws CompletionRecursionException;

    public static final int NOT_FOUND = 0;
    public static final int FOUND_TOKEN = 1;
    public static final int FOUND_BECAUSE_OF_GETATTR = 2;

    /**
     * @param ifHasGetAttributeConsiderInTokens if this true, consider that the token is in the tokens if a __getattribute__
     * is found.
     * 
     * @param completionCache cache for holding the info requested during a find tokens operation (it may have been
     *      already used in another operation, if it was part of another major operation)
     * 
     * @return whether the passed token is part of the global tokens of this module (including imported tokens) and the 
     * actual reason why it was considered there (as indicated by the constants).
     * 
     * @see #NOT_FOUND               
     * @see #FOUND_TOKEN             
     * @see #FOUND_BECAUSE_OF_GETATTR
     * 
     * @throws CompletionRecursionException 
     */
    public int isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods,
            boolean ifHasGetAttributeConsiderInTokens, ICompletionCache completionCache)
            throws CompletionRecursionException;

    /**
     * This function can be called to find possible definitions of a token (state activation token), based on its name, line and
     * column.
     * 
     * @param line: starts at 1 (-1 if not available)
     * @param col: starts at 1 (-1 if not available)
     * @param findInfo: this is debug information gathered during a find
     * @return array of definitions.
     * @throws Exception
     */
    public abstract IDefinition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature)
            throws Exception;

    /**
     * This function should return all tokens that are global for a given token.
     * E.g. if we have a class declared in the module, we return all tokens that are 'global'
     * for the class (methods and attributes).
     * 
     * @param token
     * @param manager
     * @return
     */
    public abstract IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager);

    /**
     * @return the docstring for a module.
     */
    public abstract String getDocString();

    /**
     * @return the name of the module
     */
    public abstract String getName();

    /**
     * @return the local scope in the module for a given line and column
     * May return null if no scope is found
     * @param line: starts at 0
     * @param col: starts at 0
     */
    public abstract ILocalScope getLocalScope(int line, int col);

    /**
     * @return true if this module is actually a package:
     *  - module with __init__.py for python
     *  - folder for java (not class)
     */
    public abstract boolean isPackage();

    /**
     * @return a string with the name of the folder for the package represented by this module -- usually, the
     * name of the module without the __init__.py.
     * 
     * Only actually applicable if isPackage == true
     */
    public String getPackageFolderName();

    /**
     * @return if this module has a from __future__ import absolute_import token declared.
     */
    public abstract boolean hasFutureImportAbsoluteImportDeclared();

    public IPythonNature getNature();
}