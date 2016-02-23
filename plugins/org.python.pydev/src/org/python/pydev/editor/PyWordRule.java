/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 2, 2006
 */
package org.python.pydev.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * This class is a copy of the WordRule, with the exception that when we detected a 'def' or a 'class', the next default token
 * is not the regular, but the token identifying the class name or the function name.
 * 
 * The changes were: 
 * 
 * added the classNameToken and funcNameToken attributes (and constructor parameters)
 * changed the evaluate to store the last found and return things accordingly
 * 
 * @see IWordDetector
 */
public class PyWordRule implements IRule {

    /** Internal setting for the un-initialized column constraint */
    protected static final int UNDEFINED = -1;

    /** The word detector used by this rule */
    protected IWordDetector fDetector;
    /** The default token to be returned on success and if nothing else has been specified. */
    protected IToken fDefaultToken;
    /** The column constraint */
    protected int fColumn = UNDEFINED;
    /** The table of predefined words and token for this rule */
    protected Map<String, IToken> fWords = new HashMap<String, IToken>();
    /** Buffer used for pattern detection */
    private FastStringBuffer fBuffer = new FastStringBuffer();

    private IToken classNameToken;

    private IToken funcNameToken;

    private IToken parensToken;

    private IToken operatorsToken;

    /**
     * Creates a rule which, with the help of a word detector, will return the token
     * associated with the detected word. If no token has been associated, the
     * specified default token will be returned.
     *
     * @param detector the word detector to be used by this rule, may not be <code>null</code>
     * @param defaultToken the default token to be returned on success
     *      if nothing else is specified, may not be <code>null</code>
     * @param funcNameToken 
     * @param classNameToken 
     *
     * @see #addWord(String, IToken)
     */
    public PyWordRule(IWordDetector detector, IToken defaultToken, IToken classNameToken, IToken funcNameToken,
            IToken parensToken, IToken operatorsToken) {

        Assert.isNotNull(detector);
        Assert.isNotNull(defaultToken);

        fDetector = detector;
        fDefaultToken = defaultToken;
        this.classNameToken = classNameToken;
        this.funcNameToken = funcNameToken;
        this.parensToken = parensToken;
        this.operatorsToken = operatorsToken;
    }

    /**
     * Adds a word and the token to be returned if it is detected.
     *
     * @param word the word this rule will search for, may not be <code>null</code>
     * @param token the token to be returned if the word has been found, may not be <code>null</code>
     */
    public void addWord(String word, IToken token) {
        Assert.isNotNull(word);
        Assert.isNotNull(token);

        fWords.put(word, token);
    }

    /**
     * Sets a column constraint for this rule. If set, the rule's token
     * will only be returned if the pattern is detected starting at the
     * specified column. If the column is smaller then 0, the column
     * constraint is considered removed.
     *
     * @param column the column in which the pattern starts
     */
    public void setColumnConstraint(int column) {
        if (column < 0)
            column = UNDEFINED;
        fColumn = column;
    }

    private String lastFound = "";

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();

        IToken found = null;
        switch (c) {
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
                found = this.parensToken;
                break;

            case '<':
            case '>':
            case '=':
            case '+':
            case '-':
            case '/':
            case '*':
            case '!':
            case '&':
            case '|':
            case '%':
            case '~':
            case '^':
            case ',':
                found = this.operatorsToken;
                break;
        }

        if (found != null) {
            lastFound = "";
            return found;
        }

        if (fDetector.isWordStart((char) c)) {
            if (fColumn == UNDEFINED || (fColumn == scanner.getColumn() - 1)) {

                fBuffer.clear();
                do {
                    fBuffer.append((char) c);
                    c = scanner.read();
                } while (c != ICharacterScanner.EOF && fDetector.isWordPart((char) c));
                scanner.unread();

                String str = fBuffer.toString();
                IToken token = (IToken) fWords.get(str);
                if (token != null) {
                    lastFound = str;
                    return token;
                }

                if (fDefaultToken.isUndefined())
                    unreadBuffer(scanner);

                if (lastFound.equals("def")) {
                    lastFound = str;
                    return funcNameToken;
                }
                if (lastFound.equals("class")) {
                    lastFound = str;
                    return classNameToken;
                }
                return fDefaultToken;
            }
        }

        scanner.unread();
        return Token.UNDEFINED;
    }

    /**
     * Returns the characters in the buffer to the scanner.
     *
     * @param scanner the scanner to be used
     */
    protected void unreadBuffer(ICharacterScanner scanner) {
        for (int i = fBuffer.length() - 1; i >= 0; i--)
            scanner.unread();
    }
}
