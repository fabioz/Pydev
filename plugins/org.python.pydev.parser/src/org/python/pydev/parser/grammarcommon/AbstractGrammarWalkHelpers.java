/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.Token;
import org.python.pydev.shared_core.utils.Reflection;

/**
 * Helpers to walk through the grammar.
 * 
 * @author Fabio
 */
public abstract class AbstractGrammarWalkHelpers {

    private AbstractTokenManager tokenManager;

    /**
     * An iterator that can pass through the next tokens considering indentation.
     */
    private TokensIterator tokensIterator;

    /**
     * @return The token manager.
     */
    protected final AbstractTokenManager getTokenManager() {
        if (this.tokenManager == null) {
            this.tokenManager = (AbstractTokenManager) Reflection.getAttrObj(this, "token_source", true);
        }
        return this.tokenManager;
    }

    /**
     * @return the current token
     */
    protected abstract Token getCurrentToken();

    /**
     * Sets the current token in the grammar.
     */
    protected abstract void setCurrentToken(Token t);

    /**
     * @see TokensIterator#TokensIterator(AbstractTokenManager, Token, int, boolean)
     * 
     * Note that if one request is done, another cannot be done and use the iterator, because
     * the same instance is used over and over!
     */
    protected final TokensIterator getTokensIterator(Token firstIterationToken, int tokensToIterate,
            boolean breakOnIndentsDedentsAndNewCompounds) {
        if (this.tokensIterator == null) {
            this.tokensIterator = new TokensIterator(getTokenManager(), firstIterationToken, tokensToIterate,
                    breakOnIndentsDedentsAndNewCompounds);
        } else {
            this.tokensIterator.reset(getTokenManager(), firstIterationToken, tokensToIterate,
                    breakOnIndentsDedentsAndNewCompounds);
        }
        return this.tokensIterator;
    }
}
