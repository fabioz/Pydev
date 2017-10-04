package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class PyFormatStdManageBlankLines {

    public static class LineOffsetAndInfo {

        public boolean delete;
        public int offset;
        public boolean onlyWhitespacesFound = false;
        public boolean onlyCommentsFound = false;
        public int addBlankLines;
        public final int infoFromLine; // Starts at 0

        public LineOffsetAndInfo(int offset, int currLine) {
            this.offset = offset;
            this.infoFromLine = currLine;
        }

        @Override
        public String toString() {
            return StringUtils.join(", ", "offset: " + offset, "delete: " + delete, "addBlankLines: " + addBlankLines,
                    "onlyWhitespacesFound: " + onlyWhitespacesFound,
                    "onlyCommentsFound: " + onlyCommentsFound + "\n");
        }
    }

    /**
     * Do a second pass so that the following is true:
     * - when a class is found, make sure we have 2 lines before it (and if we have more remove the excess)
     * - when a method is found, make sure we have 1 line before it (and if we have more remove the excess)
     * - inside a method, if more than 2 consecutive empty lines are found, make it only 1 line.
     *
     * From pep8:
     * - Surround top-level function and class definitions with two blank lines.
     * - Method definitions inside a class are surrounded by a single blank line.
     *
     * @param doc the original doc (before any formatting taking place -- as the previous formatting didn't change
     * lines, it may still be useful to access statements based on lines -- such as imports).
     */
    public static List<LineOffsetAndInfo> computeBlankLinesAmongMethodsAndClasses(PyFormatStd.FormatStd std,
            IDocument doc,
            FastStringBuffer initialFormatting, String delimiter) throws SyntaxErrorException {
        ParsingUtils parsingUtils = ParsingUtils.create(initialFormatting);
        char[] cs = initialFormatting.getInternalCharsArray(); // Faster access

        int matchI = -1;
        boolean onlyWhitespacesFound = true;
        boolean onlyCommentsFound = false;
        final int length = initialFormatting.length();
        int decoratorState = 0; // 0 = just found, 1 = first decorator found, 2 = first decorator processed.

        List<PyFormatStdManageBlankLines.LineOffsetAndInfo> lst = new ArrayList<>();
        lst.add(new PyFormatStdManageBlankLines.LineOffsetAndInfo(0, 0));
        int currLine = 0;

        for (int i = 0; i < length; i++) {
            char c = cs[i];

            if (c == '\r' || c == '\n') {
                // Don't let more than 2 consecutive empty lines anywhere
                // (when we find some class or method we'll add spaces accordingly
                // if needed).
                if (c == '\r' && (i + 1) < length && cs[i + 1] == '\n') {
                    i++;
                }
                currLine++;
                if (onlyWhitespacesFound && lst.size() > 2 && lst.get(lst.size() - 2).onlyWhitespacesFound) {
                    lst.get(lst.size() - 2).delete = true;
                }
                PyFormatStdManageBlankLines.LineOffsetAndInfo currLineOffsetAndInfo = lst.get(lst.size() - 1);
                currLineOffsetAndInfo.onlyWhitespacesFound = onlyWhitespacesFound;
                currLineOffsetAndInfo.onlyCommentsFound = onlyCommentsFound;
                lst.add(new PyFormatStdManageBlankLines.LineOffsetAndInfo(i + 1, currLine)); // offset == first char of the next line (could be EOF)
                onlyWhitespacesFound = true;
                onlyCommentsFound = false;
                continue;
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '#') {
                if (onlyWhitespacesFound) {
                    // If until this point we only found whitespaces, there are only comments in this line.
                    onlyCommentsFound = true;
                }
                onlyWhitespacesFound = false;
                i = parsingUtils.eatComments(null, i, false);
                continue;
            }
            if (c == '@') {
                // Decorator or matrix multiplier
                if (onlyWhitespacesFound) {
                    // Consider it a decorator
                    decoratorState = 1;
                }
            }
            onlyWhitespacesFound = false;
            matchI = -1;

            switch (c) {
                case '\'':
                case '"':
                    //ignore literals and multi-line literals.
                    i = parsingUtils.eatLiterals(null, i);
                    break;

                case 'a':
                case 'c':
                case 'd':
                case '@':
                    int j;
                    switch (c) {
                        case 'a':
                            j = ParsingUtils.matchAsyncFunction(i, cs, length);
                            if (j > 0) {
                                if (decoratorState > 0) {
                                    decoratorState = 0;
                                    matchI = -1;
                                } else {
                                    matchI = j;
                                }
                            }
                        case 'c':
                            j = ParsingUtils.matchClass(i, cs, length);
                            if (j > 0) {
                                if (decoratorState > 0) {
                                    decoratorState = 0;
                                    matchI = -1;
                                } else {
                                    matchI = j;
                                }
                            }
                            break;
                        case 'd':
                            j = ParsingUtils.matchFunction(i, cs, length);
                            if (j > 0) {
                                if (decoratorState > 0) {
                                    decoratorState = 0;
                                    matchI = -1;
                                } else {
                                    matchI = j;
                                }
                            }
                            break;
                        case '@':
                            matchI = -1;
                            if (decoratorState == 2) {
                                // Don't reset flag if multiple decorators are found.
                                // (it should only be reset when a class or method is found).
                            } else {
                                if (decoratorState == 1) {
                                    matchI = i + 1;
                                    decoratorState = 2;
                                }
                            }

                            break;
                        default:
                            throw new RuntimeException("Error, should not get here.");
                    }
                    if (matchI > 0) {
                        // When we find a class, we have to make sure that we have
                        // exactly 2 empty lines before it (keeping comment blocks before it).
                        if (lst.size() > 1) {
                            // lst.size() == 1 means first line, so, no point in adding new line
                            // lst.size() < 1 should never happen.
                            int reverseI = lst.size() - 2;
                            // lst.size() -2 is previous line and lst.size() -1 curr line
                            // so, get a comment block right before the class or def and don't
                            // split it (split before the block)
                            int foundAt = lst.size() - 1;
                            LineOffsetAndInfo currLineOffsetAndInfo = lst
                                    .get(foundAt);
                            while (reverseI >= 0) {
                                LineOffsetAndInfo prev = lst.get(reverseI);
                                if (prev.onlyCommentsFound) {
                                    currLineOffsetAndInfo = prev;
                                    foundAt = reverseI;
                                    reverseI--;
                                } else {
                                    break;
                                }
                            }
                            if (reverseI >= 0) {
                                // if reverseI < 0, we got to the top of the document only with comments
                                // don't add new lines as it's dubious whether this is from the module or from the class.
                                //
                                // i.e.:
                                // # comment
                                // # comment
                                // class Foo(object):
                                int blankLinesNeeded = std.blankLinesBeforeInnerFunc;
                                if (cs[i - 1] == '\n' || cs[i - 1] == '\r') {
                                    // top level
                                    blankLinesNeeded = std.blankLinesBeforeTopLevelClassOrFunc;

                                }
                                // Ok, now, let's see if we have the proper space
                                for (int k = foundAt; k >= 0 && blankLinesNeeded > 0; k--) {
                                    LineOffsetAndInfo info = lst.get(k);
                                    if (info.onlyWhitespacesFound && !info.delete) {
                                        blankLinesNeeded--;
                                    }
                                }

                                if (blankLinesNeeded > 0) {
                                    for (int k = foundAt; k >= 0 && blankLinesNeeded > 0; k--) {
                                        LineOffsetAndInfo info = lst.get(k);
                                        if (info.onlyWhitespacesFound && info.delete) {
                                            info.delete = false;
                                            blankLinesNeeded--;
                                        }
                                    }
                                    if (blankLinesNeeded > 0) {
                                        currLineOffsetAndInfo.addBlankLines = blankLinesNeeded;
                                    }
                                }
                            }
                        }
                        i = matchI - 1;
                    } else {
                    }
                    break;

            }
        }
        return lst;
    }

    public static FastStringBuffer fixBlankLinesAmongMethodsAndClasses(List<LineOffsetAndInfo> computed, FormatStd std,
            IDocument doc, FastStringBuffer initialFormatting, String delimiter) {
        char[] cs = initialFormatting.getInternalCharsArray(); // Faster access
        int length = initialFormatting.length();
        Iterator<LineOffsetAndInfo> it = computed.iterator();
        LineOffsetAndInfo currInfo = it.next(); // we must have at least one entry every time.
        FastStringBuffer newBuf = new FastStringBuffer(length + 20);
        for (int i = 0; i < length; i++) {
            if (i > currInfo.offset) {
                if (it.hasNext()) {
                    currInfo = it.next();
                }
            }
            char c = cs[i];
            if (i == currInfo.offset) {
                for (int j = 0; j < currInfo.addBlankLines; j++) {
                    newBuf.append(delimiter);
                }
                if (currInfo.delete) {
                    while (c != '\r' && c != '\n' && i < length) {
                        i++;
                        c = cs[i];
                    }
                    if (c == '\r' && i + 1 < length && cs[i + 1] == '\n') {
                        i++; // skip \r\n
                    }
                    continue;
                }
            }
            newBuf.append(c);
        }
        return newBuf;
    }

}
