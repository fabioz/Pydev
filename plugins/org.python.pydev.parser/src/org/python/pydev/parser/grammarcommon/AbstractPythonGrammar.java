/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.util.List;

import org.python.pydev.parser.IGrammar;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.Token;

public abstract class AbstractPythonGrammar extends AbstractGrammarErrorHandlers implements ITreeConstants, IGrammar {

    public final static boolean DEFAULT_SEARCH_ON_LAST = false;

    /**
     * @return the token at the given location in the stack.
     */
    public abstract Token getToken(int i);

    /**
     * @return the list of special added to the token manager (used so that we
     * can add more info to it later on)
     */
    public abstract List<Object> getTokenSourceSpecialTokensList();

    /**
     * @return the last pos.
     */
    protected abstract Token getJJLastPos();

    protected Object temporaryToken;

    protected final boolean generateTree;

    protected final IPythonGrammarActions grammarActions;

    public IPythonGrammarActions getGrammarActions() {
        return grammarActions;
    }

    protected AbstractPythonGrammar(boolean generateTree) {
        this.generateTree = generateTree;
        if (generateTree) {
            grammarActions = new DefaultPythonGrammarActions(this);
        } else {
            grammarActions = new NullPythonGrammarActions();
        }
    }

    private int lastLevelCol = -1;
    private int lastLevelLine = -1;

    protected void markLastImportLevelPos() {
        Token token = getToken(0);
        lastLevelCol = token.beginColumn + 1;
        lastLevelLine = token.beginLine;
    }

    protected int getLastLevelImportCol() {
        return lastLevelCol;
    }

    protected int getLastLevelImportLine() {
        return lastLevelLine;
    }

    protected static WithNameInvalidException withNameInvalidException = new WithNameInvalidException(
            "With cannot be used as identifier when future with_statement is available.");

    /**
     * Opens a node scope
     *
     * @param n the node marking the beginning of the scope.
     */
    protected final void jjtreeOpenNodeScope(Node n) {
    }

    /**
     * Closes a node scope
     *
     * @param n the node that should have its scope closed.
     * @throws ParseException
     */
    protected final void jjtreeCloseNodeScope(Node n) throws ParseException {
        grammarActions.jjtreeCloseNodeScope(n);
    }

    protected final AbstractJJTPythonGrammarState createJJTPythonGrammarState(Class<?> treeBuilderClass) {
        if (this.generateTree) {
            return new JJTPythonGrammarState(treeBuilderClass, this);
        } else {
            return new NullJJTPythonGrammarState();
        }
    }

    /**
     * Default: add after the previous found token
     */
    public static final int STRATEGY_ADD_AFTER_PREV = 0;

    /**
     * Add before the 'next token' strategy
     */
    public static final int STRATEGY_BEFORE_NEXT = 1;

    public boolean getInsideAsync() {
        return false;
    }

}
