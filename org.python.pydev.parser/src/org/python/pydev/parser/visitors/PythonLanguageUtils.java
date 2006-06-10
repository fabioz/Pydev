/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.parser.visitors;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * These are some utilities for the Python language
 */
public class PythonLanguageUtils {

    public static final String[] KEYWORDS = new String[]{
            "and",
            "assert",
            "break",
            "class",
            "continue",
            "def",
            "del",
            "elif",
            "else",
            "except",
            "exec",
            "finally",
            "for",
            "from",
            "global",
            "if",
            "import",
            "in",
            "is",
            "lambda",
            "not",
            "or",
            "pass",
            "print",
            "raise",
            "return",
            "try",
            "while",
            "yield"            
    };

    public static final SortedSet<String> KEYWORDS_SET = createKeywordsSet();

    private static SortedSet<String> createKeywordsSet() {
        TreeSet<String> set = new TreeSet<String>();
        for(String k : KEYWORDS){
            set.add(k);
        }
        return set;
    }

    public static boolean isKeyword(String selectedWord) {
        return KEYWORDS_SET.contains(selectedWord);
    }
}
