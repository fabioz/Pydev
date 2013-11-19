/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.io.IOException;

import org.python.pydev.parser.jython.FastCharStream;
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
     * @return the next available token considering new lines (just doing a getToken at any place won't always give us
     * the effect we'd usually expect -- which is finding new lines and indent tokens)
     */
    protected static Token nextTokenConsideringNewLine(ITokenManager tokenManager) {
        boolean foundNewLine = searchNewLine(tokenManager, true);
        if (foundNewLine) {
            tokenManager.indenting(0);
        }
        final Token nextToken = tokenManager.getNextToken();
        return nextToken;
    }

    /**
     * Searches for a new line in the input stream. If found, it'll stop right after it, otherwise, the stream will be
     * backed up the number of chars that've been read.
     */
    protected static boolean searchNewLine(ITokenManager tokenManager, boolean breakOnFirstNotWhitespace) {
        boolean foundNewLine = false;
        FastCharStream inputStream = tokenManager.getInputStream();
        int currentPos = inputStream.getCurrentPos();

        try {
            while (true) {
                try {
                    char c = inputStream.readChar();
                    if (c == '\r' || c == '\n') {
                        if (c == '\r') {
                            c = inputStream.readChar();
                            if (c != '\n') {
                                inputStream.backup(1);
                            }
                        }
                        foundNewLine = true;
                        break;
                    }
                    if (breakOnFirstNotWhitespace && !Character.isWhitespace(c)) {
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
            }
        } finally {
            if (!foundNewLine) {
                inputStream.restorePos(currentPos);
            }
        }
        return foundNewLine;
    }

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
