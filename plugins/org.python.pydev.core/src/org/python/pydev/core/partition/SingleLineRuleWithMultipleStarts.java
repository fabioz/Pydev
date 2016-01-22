package org.python.pydev.core.partition;

import java.util.ArrayList;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.shared_core.partitioner.IChangeTokenRule;
import org.python.pydev.shared_core.partitioner.IMarkScanner;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class SingleLineRuleWithMultipleStarts implements IPredicateRule, IChangeTokenRule {

    protected IToken fToken;
    private final char escapeCharacter;
    private final boolean escapeContinuesLine;
    private ArrayList<char[]> fStartSequences;
    private char[] fEndSequence;

    @Override
    public void setToken(IToken token) {
        this.fToken = token;
    }

    public SingleLineRuleWithMultipleStarts(String[] startSequences, String endSequence, Token token,
            char escapeCharacter,
            boolean escapeContinuesLine) {
        ArrayList<char[]> lst = new ArrayList<>(startSequences.length);
        for (String start : startSequences) {
            lst.add(start.toCharArray());
        }
        this.fStartSequences = lst;
        this.fEndSequence = endSequence.toCharArray();
        this.fToken = token;
        this.escapeCharacter = escapeCharacter;
        this.escapeContinuesLine = escapeContinuesLine;
    }

    public IToken evaluate(ICharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    public IToken getSuccessToken() {
        return fToken;
    }

    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        if (resume) {
            if (detectEnd(scanner)) {
                return fToken;
            }
        } else {
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

            //if it got here, the start was detected
            if (detectEnd(scanner)) {
                return fToken;
            } else {
                markScanner.setMark(mark);
            }
        }

        return Token.UNDEFINED;
    }

    private boolean detectEnd(ICharacterScanner scanner) {
        while (true) {
            int c = scanner.read();
            if (c == ICharacterScanner.EOF) {
                //match
                return true;

            } else if (c == escapeCharacter) {
                if (escapeContinuesLine) {
                    //Consume new line and keep on matching
                    c = scanner.read();
                    if (c == '\r') {
                        c = scanner.read();
                        if (c != '\n') {
                            scanner.unread();
                        }
                    }
                } else {
                    //Escape does not continue line: if it's a new line, match it (but don't consume it).
                    c = scanner.read();
                    if (c == '\r' || c == '\n') {
                        scanner.unread();
                    }

                    return true;
                }
            } else if (c == '\r' || c == '\n') {
                //If it's a new line, match it (but don't consume it).
                //                scanner.unread();
                return true;

            } else if (c == fEndSequence[0]) {
                // Let's check if we had a match: if we did, return true, otherwise, keep on going.

                //matched first. Let's check the remainder
                boolean found = true;
                for (int i = 1; i < fEndSequence.length; i++) {
                    c = scanner.read();
                    if (c != fEndSequence[i]) {
                        found = false;
                        scanner.unread();
                        for (int j = 0; j < i; j++) {
                            scanner.unread();
                        }
                        break;
                    }
                }
                return found;

            }
        }
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer("SingleLineRuleWithMultipleStarts(", fEndSequence.length + 40);
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
