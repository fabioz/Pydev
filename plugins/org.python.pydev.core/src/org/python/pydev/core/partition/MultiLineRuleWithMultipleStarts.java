package org.python.pydev.core.partition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.shared_core.partitioner.IChangeTokenRule;
import org.python.pydev.shared_core.partitioner.IMarkScanner;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class MultiLineRuleWithMultipleStarts implements IPredicateRule, IChangeTokenRule {

    protected IToken fToken;
    protected final List<char[]> fStartSequences;
    protected final char[] fEndSequence;
    protected final char fEscapeCharacter;

    public void setToken(IToken token) {
        this.fToken = token;
    }

    public MultiLineRuleWithMultipleStarts(String[] startSequences, String end, IToken token, char escapeCharacter) {
        ArrayList<char[]> lst = new ArrayList<>(startSequences.length);
        for (String start : startSequences) {
            lst.add(start.toCharArray());
        }
        this.fStartSequences = lst;

        this.fEndSequence = end.toCharArray();
        this.fToken = token;
        this.fEscapeCharacter = escapeCharacter;
    }

    public IToken evaluate(ICharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    public IToken getSuccessToken() {
        return fToken;
    }

    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        if (resume) {
            return Token.UNDEFINED;
        }

        IMarkScanner markScanner = (IMarkScanner) scanner;
        int mark = markScanner.getMark();
        int c;

        int size = fStartSequences.size();
        for (int j = 0; j < size; j++) {
            boolean found = true;

            char[] startSequence = fStartSequences.get(j);
            for (int i = 0; i < startSequence.length; i++) {
                c = scanner.read();
                if (c != startSequence[i]) {
                    //Backup to where we started
                    found = false;
                    markScanner.setMark(mark);
                    break;
                }
            }
            if (found) {
                break;
            } else {
                //Didn't find... go to next (unless we checked all: in this case return that
                //we didn't match the start).
                if (j == size - 1) {
                    return Token.UNDEFINED;
                }
            }
        }

        //Ok, found start sequence, now, find the end sequence.
        while (true) {
            c = scanner.read();
            if (c == ICharacterScanner.EOF) {
                return fToken; //Always match open partitions that are unclosed on a multi line rule.
            }
            if (c == fEscapeCharacter) { //skip the next char if skip char is matched
                c = scanner.read();
                if (c == ICharacterScanner.EOF) {
                    return fToken; //Always match open partitions that are unclosed on a multi line rule.
                }
                continue;
            }
            mark = markScanner.getMark();
            boolean matched = true;

            for (int i = 0;; i++) {
                if (c != fEndSequence[i]) {
                    markScanner.setMark(mark);
                    matched = false;
                    break;
                }
                if (i + 1 < fEndSequence.length) {
                    c = scanner.read();
                } else {
                    break;
                }
            }
            if (matched) {
                return fToken;
            }
        }
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer("MultiLineRuleWithMultipleStarts(", fEndSequence.length + 40);
        buf.append("start: ");
        for (char[] chars : this.fStartSequences) {
            buf.append(chars).append(",\n");
        }
        buf.append("end: ")
                .append(fEndSequence)
                .append(")");
        return buf.toString();
    }

}
