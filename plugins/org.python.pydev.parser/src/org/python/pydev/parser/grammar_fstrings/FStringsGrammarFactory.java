package org.python.pydev.parser.grammar_fstrings;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.FastCharStream;

public class FStringsGrammarFactory {

    public static FStringsGrammar createGrammar(String s) {

        List<Character> cList = new ArrayList<Character>();

        for (char sChar : charArrayUnderlinesReplaced(s.toCharArray())) {
            cList.add(sChar);
        }

        cList = removeEqualSignals(cList);

        char[] c = new char[cList.size()];
        for (int i = 0; i < cList.size(); i++) {
            c[i] = cList.get(i);
        }

        FastCharStream in = new FastCharStream(c);
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);

        return fStringsGrammar;
    }

    private static char[] charArrayUnderlinesReplaced(char[] c) {
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
        return c;
    }

    private static List<Character> removeEqualSignals(List<Character> cList) {

        int equalPos = cList.indexOf('=');
        while (equalPos != -1) {
            cList.remove(equalPos);
            equalPos = cList.indexOf('=');
        }

        return cList;
    }

}
