package org.python.pydev.parser.grammar_fstrings;

import org.python.pydev.parser.jython.FastCharStream;

public class FStringsGrammarFactory {

    public static FStringsGrammar createGrammar(String s) {

        char[] c = s.toCharArray();
        for (int i = 0, z = 0; i < c.length; i++) {
            if (c[i] == '\\' && i + 2 < c.length && c[i + 1] == 'N' && c[i + 2] == '{') {
                c[i + 2] = '_';
                i += 2;
                z = i + 1;
                for (; z < c.length; z++) {
                    if (c[z] == '}') {
                        c[z] = '_';
                        break;
                    }
                }
            }
        }

        FastCharStream in = new FastCharStream(c);
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);

        return fStringsGrammar;
    }

}
