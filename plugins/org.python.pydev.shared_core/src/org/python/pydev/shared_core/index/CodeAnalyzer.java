/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.CharTokenizer;

/**
 * The tokenizers are registered externally for this analyzer.
 */
public class CodeAnalyzer extends Analyzer {

    public CodeAnalyzer() {
        super();
        fieldNameToStreamComponents.put("__default__", createDefaultComponents());
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        TokenStreamComponents streamComponents = fieldNameToStreamComponents.get(fieldName);
        if (streamComponents != null) {
            return streamComponents;
        }
        return fieldNameToStreamComponents.get("__default__");
    }

    Map<String, TokenStreamComponents> fieldNameToStreamComponents = new HashMap<>();

    public void registerTokenizer(String fieldName, TokenStreamComponents tokenStream) {
        fieldNameToStreamComponents.put(fieldName, tokenStream);
    }

    // Code in general
    public static TokenStreamComponents createDefaultComponents(String... ignoreWords) {
        Tokenizer src = new CharTokenizer() {

            @Override
            protected boolean isTokenChar(int c) {
                return Character.isJavaIdentifierPart(c);
            }

            @Override
            protected int normalize(int c) {
                return Character.toLowerCase(c);
            }
        };

        TokenFilter tok = new LowerCaseFilter(src);
        CharArraySet stopWords = StopFilter.makeStopSet(ignoreWords);
        tok = new StopFilter(tok, stopWords);

        TokenStreamComponents tokenStreamComponents = new TokenStreamComponents(src, tok);
        return tokenStreamComponents;
    }

    // Python-related
    private static final String[] PYTHON_KEYWORDS = new String[] {
            "False", "None", "True", "and", "as", "assert",
            "break", "class", "continue", "def", "del", "elif",
            "else", "except", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while",
            "with", "yield" };

    public static TokenStreamComponents createPythonStreamComponents() {
        return createDefaultComponents(PYTHON_KEYWORDS);
    }

    // Things to ignore in comments/strings
    private static final String[] GENERAL_STOP_WORDS = {
            "a", "an", "and", "are", "as", "at", "be", "but",
            "by", "for", "if", "in", "into", "is", "it", "i",
            "no", "not", "of", "on", "or", "s", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with", "we", "you" };

    public static TokenStreamComponents createStringsOrCommentsStreamComponents() {
        return createDefaultComponents(GENERAL_STOP_WORDS);
    }

}
