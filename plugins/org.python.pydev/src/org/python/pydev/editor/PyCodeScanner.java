/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: atotic, Scott Schlesier
 * Created: March 5, 2005
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * PyCodeScanner - A scanner that looks for python keywords and code
 * and supports the updating of named colors through the colorCache
 * 
 * GreatWhite, GreatKeywordDetector came from PyEditConfiguration
 */
public class PyCodeScanner extends RuleBasedScanner {

    // keywords list has to be alphabetized for the keyword detector to work properly
    static final public String[] DEFAULT_KEYWORDS = { "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def",
            "del", "elif", "else", "except", "exec", "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "nonlocal", "not", "or", "pass", "print", "raise", "return", "self", "try", "while", "with",
            "yield", "False", "None", "True",

    };

    static public String[] CYTHON_KEYWORDS;

    static {
        CYTHON_KEYWORDS = new String[] { "cimport", "cdef", "ctypedef" };
        CYTHON_KEYWORDS = ArrayUtils.concatArrays(DEFAULT_KEYWORDS, CYTHON_KEYWORDS);
        // keywords list has to be alphabetized for the keyword detector to work properly
        Arrays.sort(CYTHON_KEYWORDS);
    }

    private final ColorAndStyleCache colorCache;

    private IToken keywordToken;
    private IToken selfToken;
    private IToken defaultToken;
    private IToken decoratorToken;
    private IToken numberToken;
    private IToken classNameToken;
    private IToken funcNameToken;
    private IToken parensToken;
    private IToken operatorsToken;

    private String[] keywords;

    private ICodeScannerKeywords codeScannerKeywords;

    /**
     * Whitespace detector.
     * 
     * I know, naming the class after a band that burned
     * is not funny, but I've got to get my brain off my
     * annoyance with the abstractions of JFace.
     * So many classes and interfaces for a single method?
     * f$%@#$!!
     */
    static private class GreatWhite implements IWhitespaceDetector {
        @Override
        public boolean isWhitespace(char c) {
            return Character.isWhitespace(c);
        }
    }

    /**
     * Python keyword detector
     */
    static private class GreatKeywordDetector implements IWordDetector {

        public GreatKeywordDetector() {
        }

        @Override
        public boolean isWordStart(char c) {
            return Character.isJavaIdentifierStart(c);
        }

        @Override
        public boolean isWordPart(char c) {
            return Character.isJavaIdentifierPart(c);
        }
    }

    static private class DecoratorDetector implements IWordDetector {

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        @Override
        public boolean isWordStart(char c) {
            return c == '@';
        }

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
         */
        @Override
        public boolean isWordPart(char c) {
            return c != '\n' && c != '\r' && c != '(';
        }

    }

    static public class NumberDetector implements IWordDetector {

        /**
         * Used to keep the state of the token
         */
        private FastStringBuffer buffer = new FastStringBuffer();

        /**
         * Defines if we are at an hexa number
         */
        private boolean isInHexa;

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        @Override
        public boolean isWordStart(char c) {
            isInHexa = false;
            buffer.clear();
            buffer.append(c);
            return Character.isDigit(c);
        }

        /**
         * Check if we are still in the number
         */
        @Override
        public boolean isWordPart(char c) {
            //ok, we have to test for scientific notation e.g.: 10.9e10

            if ((c == 'x' || c == 'X') && buffer.length() == 1 && buffer.charAt(0) == '0') {
                //it is an hexadecimal
                buffer.append(c);
                isInHexa = true;
                return true;
            } else {
                buffer.append(c);
            }

            if (isInHexa) {
                return Character.isDigit(c) || c == 'a' || c == 'A' || c == 'b' || c == 'B' || c == 'c' || c == 'C'
                        || c == 'd' || c == 'D' || c == 'e' || c == 'E' || c == 'f' || c == 'F';

            } else {
                return Character.isDigit(c) || c == 'e' || c == '.';
            }
        }

    }

    public PyCodeScanner(ColorAndStyleCache colorCache) {
        this(colorCache, DEFAULT_KEYWORDS);
    }

    public PyCodeScanner(ColorAndStyleCache colorCache, String[] keywords) {
        super();
        this.keywords = keywords;
        this.colorCache = colorCache;

        setupRules();
    }

    /**
     * @param colorCache2
     * @param codeScannerKeywords
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PyCodeScanner(ColorAndStyleCache colorCache, ICodeScannerKeywords codeScannerKeywords) {
        super();
        this.colorCache = colorCache;
        this.codeScannerKeywords = codeScannerKeywords;
        this.keywords = codeScannerKeywords.getKeywords();

        setupRules();

        codeScannerKeywords.getOnChangeCallbackWithListeners().registerListener(new ICallbackListener() {

            @Override
            public Object call(Object obj) {
                keywords = PyCodeScanner.this.codeScannerKeywords.getKeywords();
                setupRules();
                return null;
            }
        });
    }

    public void updateColors() {
        setupRules();
    }

    private void setupRules() {
        keywordToken = new Token(colorCache.getKeywordTextAttribute());

        selfToken = new Token(colorCache.getSelfTextAttribute());

        defaultToken = new Token(colorCache.getCodeTextAttribute());

        decoratorToken = new Token(colorCache.getDecoratorTextAttribute());

        numberToken = new Token(colorCache.getNumberTextAttribute());

        classNameToken = new Token(colorCache.getClassNameTextAttribute());

        funcNameToken = new Token(colorCache.getFuncNameTextAttribute());

        parensToken = new Token(colorCache.getParensTextAttribute());

        operatorsToken = new Token(colorCache.getOperatorsTextAttribute());

        setDefaultReturnToken(defaultToken);
        List<IRule> rules = new ArrayList<IRule>();

        // Scanning strategy:
        // 1) whitespace
        // 2) code
        // 3) regular words?

        WhitespaceRule whitespaceRule;
        try {
            whitespaceRule = new WhitespaceRule(new GreatWhite(), defaultToken);
        } catch (Throwable e) {
            //Compatibility with Eclipse 3.4 and below.
            whitespaceRule = new WhitespaceRule(new GreatWhite());
        }
        rules.add(whitespaceRule);

        Map<String, IToken> defaults = new HashMap<String, IToken>();
        defaults.put("self", selfToken);

        PyWordRule wordRule = new PyWordRule(new GreatKeywordDetector(), defaultToken, classNameToken, funcNameToken,
                parensToken, operatorsToken);
        for (String keyword : keywords) {
            IToken token = defaults.get(keyword);
            if (token == null) {
                token = keywordToken;
            }
            wordRule.addWord(keyword, token);
        }

        rules.add(wordRule);

        rules.add(new WordRule(new DecoratorDetector(), decoratorToken));
        rules.add(new WordRule(new NumberDetector(), numberToken));

        setRules(rules.toArray(new IRule[0]));
    }

    /**
     * Used from the django templates editor.
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
        this.setupRules();
    }
}
