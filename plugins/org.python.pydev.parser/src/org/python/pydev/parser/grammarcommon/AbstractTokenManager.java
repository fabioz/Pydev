/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.shared_core.utils.Reflection;

/**
 * 
 * Note that this class actually has a tight coupling with subclasses, searching directly for some attributes
 * (e.g.: curLexState so that we don't need to override a get for each token manager class)
 *
 *
 * @author Fabio
 *
 */
public abstract class AbstractTokenManager extends AbstractTokenManagerWithConstants implements ITreeConstants,
        ITokenManager {

    /**
     * A stack with the indentations... No sure why it's not a stack (indentation+level)
     */
    protected final IndentLevel indentation = new IndentLevel();

    /**
     * When we find an EOF, we create artificially all the dedents. So, if we need to go back, we might
     * need to get back to the point we were previously.
     */
    protected int levelBeforeEof = -1;

    /**
     * If > 0, we're in a (, [ or {
     */
    protected int parens = 0;

    /**
     * This is the indentation in the current line (number of chars found after a \n)
     */
    protected int indent;

    /**
     * The special tokens available.
     */
    public final List<Object> specialTokens = new ArrayList<Object>();

    /**
     * The input stream.
     */
    private FastCharStream inputStream;

    public final FastCharStream getInputStream() {
        if (this.inputStream == null) {
            this.inputStream = (FastCharStream) Reflection.getAttrObj(this, "input_stream", true);
        }
        return inputStream;
    }

    //must be calculated
    protected final int getCurLexState() {
        return (Integer) Reflection.getAttrObj(this, "curLexState", true);
    }

    /**
     * Gets the next token. Note that indents and dedents can be skipped if we're not currently
     * in a new line (that's the reason for a number of tricks while trying to recover from errors)
     */
    public abstract Token getNextToken();

    protected AbstractTokenManager() {
    }

    /**
     * Creates a new token of the given kind (with the given image) in the same position as the 
     * previous token passed (note that even endline and endColumn are in the same position...
     * even if that'd be strange, that's what we want because those positions can be used
     * later and we need that specific point in the grammar)
     */
    protected final Token createFrom(Token previous, int kind, String image) {
        Token t = new Token();
        t.kind = kind;
        t.beginLine = previous.beginLine;
        t.endLine = previous.endLine;
        t.beginColumn = previous.beginColumn;
        t.endColumn = previous.endColumn;
        t.image = image;
        t.specialToken = null;
        t.next = null;
        return t;
    }

    /**
     * Creates a new token based on the coordinates on the previous and sets it as the next from the previous
     */
    protected final Token createFromAndSetAsNext(Token previous, int kind, String image) {
        Token t = createFrom(previous, kind, image);
        Token oldNext = previous.next;
        previous.next = t;
        t.next = oldNext;
        return t;
    }

    /**
     * Adds and returns a dedent token
     */
    protected final Token addDedent(Token previous) {
        return createFromAndSetAsNext(previous, getDedentId(), "<DEDENT>");
    }

    /**
     * Map pointing a token representation (, [, {, :, etc. to its id in the grammar.
     */
    private Map<String, Integer> tokenToIdCache;

    /**
     * @return a map pointing a token representation (, [, {, :, etc. to its id in the grammar.
     */
    private Map<String, Integer> getTokenToId() {
        if (tokenToIdCache == null) {
            tokenToIdCache = new HashMap<String, Integer>();

            tokenToIdCache.put(")", getRparenId());
            tokenToIdCache.put("]", getRbracketId());
            tokenToIdCache.put("}", getRbraceId());

            tokenToIdCache.put(":", getColonId());
            tokenToIdCache.put("(", getLparenId());
            tokenToIdCache.put(",", getCommaId());
        }
        return tokenToIdCache;
    }

    /**
     * Identifies that a custom token could not be created.
     */
    public static int CUSTOM_NOT_CREATED = 0;

    /**
     * Identifies that a custom token was created and it wasn't a parens
     * (meaning that we didn't change the parens level by creating it)
     */
    public static int CUSTOM_CREATED_NOT_PARENS = 1;

    /**
     * Identifies that a custom token was created and it wasn a parens
     * (meaning that we did change the parens level by creating it -- which
     * might requiring getting back in the input source to recreate possibly skipped
     * indents and dedents)
     */
    public static int CUSTOM_CREATED_WAS_PARENS = 2;

    /**
     * Creates a custom token for the given token representation (if possible)
     * 
     * @see #CUSTOM_NOT_CREATED
     * @see #CUSTOM_CREATED_NOT_PARENS
     * @see #CUSTOM_CREATED_WAS_PARENS
     */
    public int addCustom(Token curr, String token) {
        Integer id = getTokenToId().get(token);
        if (id != null) {
            createFromAndSetAsNext(curr, id, token);
            int ret = CUSTOM_CREATED_NOT_PARENS;
            if (id == getRparenId() || id == getRbracketId() || id == getRbraceId()) {
                parens--;
                ret = CUSTOM_CREATED_WAS_PARENS;
            } else if (id == getLparenId()) {
                parens++;
                ret = CUSTOM_CREATED_WAS_PARENS;
            }
            return ret;
        }
        return CUSTOM_NOT_CREATED;
    }

    /**
     * Called right after the creation of any token.
     * 
     * We may need to add the special tokens found while parsing to it and
     * add dedents (otherwise the grammar won't finish successfully if we don't have
     * a new line in the end).
     */
    protected final void CommonTokenAction(final Token initial) {
        Token t = initial;

        int i = specialTokens.size();
        while (t.specialToken != null) {
            this.specialTokens.add(i, t.specialToken);
            t = t.specialToken;
        }

        //Now, we must check the actual token here for EOF. 
        t = initial;

        //This is the place we check if we have to add dedents so that the parsing ends 'gracefully' when
        //we find and EOF.
        if (t.kind == getEofId()) {
            //Store it because if we backtrack we have to restore it!!
            this.levelBeforeEof = indentation.level;
            if (getCurLexState() == getLexerDefaultId()) {
                t.kind = getNewlineId();
            } else {
                t.kind = getDedentId();
                if (indentation.level >= 0) {
                    indentation.level -= 1;
                }
            }
            while (indentation.level >= 0) {
                indentation.level--;
                t = addDedent(t);
            }
            t.kind = getEofId();
            t.image = "<EOF>";
        }
    }

    /**
     * Must be called right after a new line with 0 as a parameter. Identifies the number of whitespaces in the current line.
     */
    public abstract void indenting(int i);

    /**
     * @return The current level of the indentation in the current line.
     */
    public abstract int getCurrentLineIndentation();

    /**
     * @return The current level of the indentation.
     */
    public abstract int getLastIndentation();

}
