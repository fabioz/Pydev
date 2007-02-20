/*
 * Created on Sep 23, 2006
 * @author Fabio
 */
package org.python.pydev.core;

import java.util.List;

import org.python.pydev.core.structure.FastStack;

public interface ILocalScope {

    /**
     * Checks if this scope is an outer scope of the scope passed as a param (s).
     * Or if it is the same scope. 
     */
    public boolean isOuterOrSameScope(ILocalScope s);

    /**
     * @return all the local tokens found 
     */
    public IToken[] getAllLocalTokens();

    /**
     * @param endLine tokens will only be recognized if its beginLine is higher than this parameter.
     */
    public IToken[] getLocalTokens(int endLine, int col, boolean onlyArgs);

    /**
     * @param line: starts at 1
     * @param col: starts at 1
     * @return the modules that are imported in the current (local) scope as tokens
     */
    public List<IToken> getLocalImportedModules(int line, int col, String moduleName);

    /**
     * @return whether the last element found in this scope is a class definition
     */
    public boolean isLastClassDef();

    /**
     * @return the scope stack with simple nodes
     * @note SimpleNode is not declared because we only have it in the parser (and not in the local scope)
     */
    public FastStack /*<SimpleNode>*/ getScopeStack();

}