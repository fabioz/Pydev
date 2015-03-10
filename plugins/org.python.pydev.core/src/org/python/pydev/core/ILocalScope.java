/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 23, 2006
 * @author Fabio
 */
package org.python.pydev.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.FastStack;

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
    public FastStack /*<SimpleNode>*/getScopeStack();

    /**
     * @return the list of tokens that are part of the interface for some local variable.
     * E.g.:
     *
     * foo.bar
     * foo.kkk
     *
     * a token for 'bar' and a token for 'kkk' will be returned
     */
    public Collection<IToken> getInterfaceForLocal(String activationToken);

    /**
     * @return Iterator for the nodes in the scope (starting with the last to the first -- or from the inner to the outer)
     */
    public Iterator /*<SimpleNode>*/iterator();

    /**
     * @return the class definition found previously in the scope
     */
    public Object /*ClassDef*/getClassDef();

    public int getScopeEndLine();

    public int getIfMainLine();

    public void setIfMainLine(int original);

    public void setScopeEndLine(int beginLine);

    /**
     * @param activationToken the activation token we're looking for.
     *
     * @return a list of Strings with the new activation token that we should look for instead of the old activation token
     * if we're able to find an assert isinstance(xxx, SomeClass) -- which in this case would return SomeClass.
     * Or null if it's not able to find such a statement.
     *
     * Also can check other things (such as docstrings).
     */
    public List<String> getPossibleClassesForActivationToken(String activationToken);

    public void setFoundAtASTNode(ISimpleNode node);

    public ISimpleNode getFoundAtASTNode();
}