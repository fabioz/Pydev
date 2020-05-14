package org.python.pydev.parser.grammar_fstrings;

import org.python.pydev.parser.jython.FastCharStream;

public class FStringsGrammarFactory {

    public static FStringsGrammar createGrammar(String s) {

        while (s.contains("\\N{")) {
            int pos = s.indexOf("\\N{") + 3;
            int posLength = s.substring(0, pos).length();
            s = s.replace(s.charAt(pos), '_');
            String s_ = s.substring(pos);
            pos = s_.indexOf("}");

            s = s.replace(s.charAt(pos + posLength), '_');
        }

        FastCharStream in = new FastCharStream(s.toCharArray());
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);

        return fStringsGrammar;
    }

}
