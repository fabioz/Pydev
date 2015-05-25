/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.shared_core.log.Log;

/**
 * A generic scanner which can be "programmed" with a sequence of rules.
 * The scanner is used to get the next token by evaluating its rule in sequence until
 * one is successful. If a rule returns a token which is undefined, the scanner will proceed to
 * the next rule. Otherwise the token provided by the rule will be returned by
 * the scanner. If no rule returned a defined token, this scanner returns a token
 * which returns <code>true</code> when calling <code>isOther</code>, unless the end
 * of the file is reached. In this case the token returns <code>true</code> when calling
 * <code>isEOF</code>.
 *
 * @see IRule
 */
public abstract class AbstractCustomRuleBasedScanner implements ICharacterScanner, ITokenScanner, IDocumentScanner {

    /** The list of rules of this scanner */
    protected IRule[] fRules;
    /** The token to be returned by default if no rule fires */
    protected IToken fDefaultReturnToken;
    /** The document to be scanned */
    protected IDocument fDocument;
    /** The cached legal line delimiters of the document */
    protected char[][] fDelimiters;
    /** The offset of the next character to be read */
    protected int fOffset;
    /** The end offset of the range to be scanned */
    protected int fRangeEnd;
    /** The offset of the last read token */
    protected int fTokenOffset;
    /** The cached column of the current scanner position */
    protected int fColumn;
    /** Internal setting for the un-initialized column cache. */
    protected static final int UNDEFINED = -1;

    /**
     * Creates a new rule based scanner which does not have any rule.
     */
    public AbstractCustomRuleBasedScanner() {
    }

    /**
     * Configures the scanner with the given sequence of rules.
     * 
     * @param rules the sequence of rules controlling this scanner (can be null).
     * @note the rules may be null and a reference to them will be kept (i.e.: the
     * passed array should not be modified outside of this method).
     */
    public void setRules(IRule[] rules) {
        fRules = rules;
    }

    /**
     * Configures the scanner's default return token. This is the token
     * which is returned when none of the rules fired and EOF has not been
     * reached.
     *
     * @param defaultReturnToken the default return token
     * @since 2.0
     */
    public void setDefaultReturnToken(IToken defaultReturnToken) {
        Assert.isNotNull(defaultReturnToken.getData());
        fDefaultReturnToken = defaultReturnToken;
        if (IDocument.DEFAULT_CONTENT_TYPE.equals(fDefaultReturnToken.getData())) {
            fDefaultReturnToken = new Token(null);
            Log.log("Not sure why setting the default is not good... we should not set anything in this case and return a Token with null data.");
        }
    }

    /*
     * @see ITokenScanner#setRange(IDocument, int, int)
     */
    public void setRange(final IDocument document, int offset, int length) {
        Assert.isLegal(document != null);
        final int documentLength = document.getLength();
        checkRange(offset, length, documentLength);

        fDocument = document;
        fOffset = offset;
        fColumn = UNDEFINED;
        fRangeEnd = offset + length;

        String[] delimiters = fDocument.getLegalLineDelimiters();
        fDelimiters = new char[delimiters.length][];
        for (int i = 0; i < delimiters.length; i++) {
            fDelimiters[i] = delimiters[i].toCharArray();
        }

        if (fDefaultReturnToken == null) {
            fDefaultReturnToken = new Token(null);
        }
    }

    /**
     * Checks that the given range is valid.
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=69292
     *
     * @param offset the offset of the document range to scan
     * @param length the length of the document range to scan
     * @param documentLength the document's length
     * @since 3.3
     */
    private void checkRange(int offset, int length, int documentLength) {
        Assert.isLegal(offset > -1);
        Assert.isLegal(length > -1);
        Assert.isLegal(offset + length <= documentLength);
    }

    /*
     * @see ITokenScanner#getTokenOffset()
     */
    public int getTokenOffset() {
        return fTokenOffset;
    }

    /*
     * @see ITokenScanner#getTokenLength()
     */
    public int getTokenLength() {
        if (fOffset < fRangeEnd) {
            return fOffset - getTokenOffset();
        }
        return fRangeEnd - getTokenOffset();
    }

    /*
     * @see ICharacterScanner#getColumn()
     */
    public int getColumn() {
        if (fColumn == UNDEFINED) {
            try {
                int line = fDocument.getLineOfOffset(fOffset);
                int start = fDocument.getLineOffset(line);

                fColumn = fOffset - start;

            } catch (BadLocationException ex) {
            }
        }
        return fColumn;
    }

    /*
     * @see ICharacterScanner#getLegalLineDelimiters()
     */
    public char[][] getLegalLineDelimiters() {
        return fDelimiters;
    }

    /*
     * @see ITokenScanner#nextToken()
     *
     * Important: subclasses must do as the first thing:
     *  lastToken = null; //reset the last token
     *
     *  //Check if we looked ahead and already resolved something.
     *  if (lookAhead != null) {
     *      lastToken = lookAhead;
     *      lookAhead = null;
     *      return lastToken.token;
     *  }
     *
     */
    public IToken nextToken() {
        //Treat case where we have no rules (read to the end).
        if (fRules == null) {
            int c;
            if ((c = read()) == EOF) {
                return Token.EOF;
            } else {
                while (true) {
                    c = read();
                    if (c == EOF) {
                        unread();
                        return fDefaultReturnToken;
                    }
                }
            }
        }

        fTokenOffset = fOffset;
        fColumn = UNDEFINED;

        int length = fRules.length;
        for (int i = 0; i < length; i++) {
            IToken token = (fRules[i].evaluate(this));
            if (token == null) {
                Log.log("Error: rule " + fRules[i] + " returned a null token.");
                continue;
            }
            if (!token.isUndefined()) {
                return token;
            }
        }

        int c = read();
        if (c == EOF) {
            return Token.EOF;
        }

        return fDefaultReturnToken;
    }

    /*
     * @see ICharacterScanner#read()
     */
    public abstract int read();

    /*
     * @see ICharacterScanner#unread()
     */
    public abstract void unread();
}
