/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;
import java.util.List;

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
     * @param line
     * @param col
     * @return
     */
    public abstract IToken[] getLocalTokens(int line, int col);

    public abstract boolean isInGlobalTokens(String tok, IPythonNature nature);

    /**
     * @param tok the token we are looking for
     * @return whether the passed token is part of the global tokens of this module (including imported tokens).
     */
    public abstract boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods);

    /**
     * @param ifHasGetAttributeConsiderInTokens if this true, consider that the token is in the tokens if a __getattribute__
     * is found.
     * 
     * @return whether the passed token is part of the global tokens of this module (including imported tokens).
     */
    public boolean isInGlobalTokens(String tok, IPythonNature nature, boolean searchSameLevelMods, boolean ifHasGetAttributeConsiderInTokens);
    
    /**
     * This function can be called to find possible definitions of a token (state activation token), based on its name, line and
     * column.
     * 
     * @param findInfo: this is debug information gathered during a find
     * @return array of definitions.
     * @throws Exception
     */
    public abstract IDefinition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature, List<FindInfo> findInfo) throws Exception;

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
     */
    public abstract ILocalScope getLocalScope(int line, int col);

}