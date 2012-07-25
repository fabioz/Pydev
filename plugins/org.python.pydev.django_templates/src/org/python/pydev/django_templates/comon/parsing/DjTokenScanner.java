/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.comon.parsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.python.pydev.django_templates.common.parsing.lexer.DjangoTemplatesTokens;

public class DjTokenScanner {

    @SuppressWarnings("nls")
    private static final String[] DJ_START = { "{%", "{{" };

    @SuppressWarnings("nls")
    private static final String[] DJ_END = new String[] { "%}", "}}" };

    public List<IRule> getDjRules() {
        ArrayList<IRule> rules = new ArrayList<IRule>();
        // adds rules for finding the django templates start and end sequences
        WordRule wordRule = new WordRule(new DjStartDetector(), Token.UNDEFINED);
        IToken token = createToken(getTokenName(DjangoTemplatesTokens.DJ_START));
        for (String word : DJ_START) {
            wordRule.addWord(word, token);
        }
        rules.add(wordRule);
        wordRule = new WordRule(new DjEndDetector(), Token.UNDEFINED);
        token = createToken(getTokenName(DjangoTemplatesTokens.DJ_END));
        for (String word : DJ_END) {
            wordRule.addWord(word, token);
        }
        rules.add(wordRule);
        return rules;
    }

    private static final class DjStartDetector implements IWordDetector {

        public boolean isWordPart(char c) {
            switch (c) {
                case '{':
                case '%':
                    return true;
            }
            return false;
        }

        public boolean isWordStart(char c) {
            return c == '{';
        }
    }

    private static final class DjEndDetector implements IWordDetector {

        public boolean isWordPart(char c) {
            switch (c) {
                case '%':
                case '}':
                    return true;
            }
            return false;
        }

        public boolean isWordStart(char c) {
            return c == '}' || c == '%';
        }
    }

    protected IToken createToken(String string) {
        return new Token(string);
    }

    private static String getTokenName(short token) {
        return DjangoTemplatesTokens.getTokenName(token);
    }

}
