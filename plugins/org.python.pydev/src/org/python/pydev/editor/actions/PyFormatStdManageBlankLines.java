package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

public class PyFormatStdManageBlankLines {

    public static class LineOffsetAndInfo {

        public boolean delete;
        public int offset;
        public boolean onlyWhitespacesFound = false;
        public boolean onlyCommentsFound = false;
        public int addBlankLines;
        public final int infoFromRealLine; // Starts at 0
        public final int infoFromLogicalLine; // Starts at 0

        public LineOffsetAndInfo(int offset, int currLogicalLine, int currRealLine) {
            this.offset = offset;
            Assert.isTrue(currRealLine >= currLogicalLine);
            this.infoFromLogicalLine = currLogicalLine;
            this.infoFromRealLine = currRealLine;
        }

        @Override
        public String toString() {
            return StringUtils.join(", ", "offset: " + offset, "delete: " + delete,
                    "logic line: " + infoFromLogicalLine,
                    "real line: " + infoFromRealLine,
                    "addBlankLines: " + addBlankLines,
                    "onlyWhitespacesFound: " + onlyWhitespacesFound,
                    "onlyCommentsFound: " + onlyCommentsFound + "\n");
        }
    }

    private final FormatStd std;
    private final IDocument doc;
    private final FastStringBuffer initialFormatting;
    private final String delimiter;
    private final ParsingUtils parsingUtils;
    private final char[] cs;
    private final FastStringBuffer tempBuf;
    private final int length;

    private int offset = 0;
    private int matchI = -1;
    private boolean onlyWhitespacesFound = true;
    private boolean onlyCommentsFound = false;
    private int decoratorState = 0; // 0 = just found, 1 = first decorator found, 2 = first decorator processed.
    private int currLogicLine = 0;
    private int currRealLine = 0;

    private final FastStack<Integer> nextScopeStartRealLineTopLevel = new FastStack<>(2);
    private final FastStack<Integer> nextScopeStartRealLineInnerLevel = new FastStack<>(10);
    private final List<PyFormatStdManageBlankLines.LineOffsetAndInfo> logicalLinesInfo = new ArrayList<>();

    private PyFormatStdManageBlankLines(FormatStd std, FastStringBuffer initialFormatting,
            String delimiter) {
        this.std = std;
        this.doc = new Document(initialFormatting.toString());
        this.initialFormatting = initialFormatting;
        this.delimiter = delimiter;
        parsingUtils = ParsingUtils.create(initialFormatting);
        cs = initialFormatting.getInternalCharsArray(); // Faster access
        tempBuf = new FastStringBuffer();
        length = initialFormatting.length();

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
     *
     * @note this method computes the info, but fixBlankLinesAmongMethodsAndClasses actually does the
     * apply (this is more complicated than it apparently needs because we want to apply only to
     * changed lines, so, we create a temporary structure which tries to keep existing lines
     * as much as possible to know what can be actually changed).
     */
    public static List<LineOffsetAndInfo> computeBlankLinesAmongMethodsAndClasses(PyFormatStd.FormatStd std,
            FastStringBuffer initialFormatting, String delimiter) throws SyntaxErrorException {
        PyFormatStdManageBlankLines fmt = new PyFormatStdManageBlankLines(std, initialFormatting, delimiter);
        return fmt.computeBlankLinesAmongMethodsAndClassesInternal();
    }

    private List<LineOffsetAndInfo> computeBlankLinesAmongMethodsAndClassesInternal() throws SyntaxErrorException {
        logicalLinesInfo.add(new PyFormatStdManageBlankLines.LineOffsetAndInfo(0, 0, 0));

        for (; offset < length; offset++) {
            char c = cs[offset];

            if (c == '\r' || c == '\n') {
                boolean sameLogicLine = (offset > 0 && cs[offset - 1] == '\\');
                // Don't let more than 2 consecutive empty lines anywhere
                // (when we find some class or method we'll add spaces accordingly
                // if needed).
                if (c == '\r' && (offset + 1) < length && cs[offset + 1] == '\n') {
                    offset++;
                }
                currRealLine++;
                if (!sameLogicLine) {
                    // Only raise line if we didn't end with a '\\' (we're still in the same logic line).
                    currLogicLine++;
                    if (onlyWhitespacesFound && logicalLinesInfo.size() > 2
                            && logicalLinesInfo.get(logicalLinesInfo.size() - 2).onlyWhitespacesFound) {
                        logicalLinesInfo.get(logicalLinesInfo.size() - 2).delete = true;
                    }
                    PyFormatStdManageBlankLines.LineOffsetAndInfo currLineOffsetAndInfo = logicalLinesInfo
                            .get(logicalLinesInfo.size() - 1);
                    currLineOffsetAndInfo.onlyWhitespacesFound = onlyWhitespacesFound;
                    currLineOffsetAndInfo.onlyCommentsFound = onlyCommentsFound;

                    // offset == first char of the next line (could be EOF)
                    logicalLinesInfo.add(
                            new PyFormatStdManageBlankLines.LineOffsetAndInfo(offset + 1, currLogicLine, currRealLine));

                    onlyWhitespacesFound = true;
                    onlyCommentsFound = false;
                }
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
                offset = parsingUtils.eatComments(null, offset, false);
                continue;
            }
            if (c == '@') {
                // Decorator or matrix multiplier
                if (onlyWhitespacesFound && decoratorState == 0) {
                    // Consider it a decorator
                    decoratorState = 1;
                }
            }
            final boolean onlyWhitespacesFoundInCurrLine = onlyWhitespacesFound;
            onlyWhitespacesFound = false;
            matchI = -1;

            switch (c) {
                case '\'':
                case '"':
                    //ignore literals and multi-line literals.
                    offset = parsingUtils.eatLiterals(tempBuf.clear(), offset);
                    int count = tempBuf.countNewLines();
                    currLogicLine += count;
                    currRealLine += count;
                    break;

                case 'a':
                case 'c':
                case 'd':
                case '@':
                    if (onlyWhitespacesFoundInCurrLine) {
                        int matchOffset;
                        switch (c) {
                            case 'a':
                                matchOffset = ParsingUtils.matchAsyncFunction(offset, cs, length);
                                onMatchDefinition(matchOffset);
                            case 'c':
                                matchOffset = ParsingUtils.matchClass(offset, cs, length);
                                onMatchDefinition(matchOffset);
                                break;
                            case 'd':
                                matchOffset = ParsingUtils.matchFunction(offset, cs, length);
                                onMatchDefinition(matchOffset);
                                break;
                            case '@':
                                matchI = -1;
                                if (decoratorState == 2) {
                                    // Don't reset flag if multiple decorators are found.
                                    // (it should only be reset when a class or method is found).
                                } else {
                                    if (decoratorState == 1) {
                                        matchI = offset + 1;
                                        decoratorState = 2;
                                    }
                                }

                                break;
                            default:
                                throw new RuntimeException("Error, should not get here.");
                        }
                        if (matchI > 0 && offset > 0) {
                            int blankLinesNeeded = std.blankLinesInner;
                            if (cs[offset - 1] == '\n' || cs[offset - 1] == '\r') {
                                // top level
                                blankLinesNeeded = std.blankLinesTopLevel;
                            }

                            // When we find a class, we have to make sure that we have
                            // exactly 2 empty lines before it (keeping comment blocks before it).
                            markBlankLinesNeededAt(currLogicLine, blankLinesNeeded);
                            offset = matchI - 1;
                        }
                        break;
                    }

            }

            if (!nextScopeStartRealLineTopLevel.empty() && currRealLine >= nextScopeStartRealLineTopLevel.peek()) {
                nextScopeStartRealLineTopLevel.pop();
                nextScopeStartRealLineInnerLevel.clear(); // if a top-level was found, the inner should be cleared.
                markBlankLinesNeededAt(currLogicLine, std.blankLinesTopLevel);
            } else {
                if (!nextScopeStartRealLineInnerLevel.empty()
                        && currRealLine >= nextScopeStartRealLineInnerLevel.peek()) {
                    int endsAtRealLine = nextScopeStartRealLineInnerLevel.pop();
                    while (!nextScopeStartRealLineInnerLevel.empty()
                            && nextScopeStartRealLineInnerLevel.peek() <= endsAtRealLine) {
                        // if we have many ending at the same line, consume them.
                        nextScopeStartRealLineInnerLevel.pop();
                    }
                    markBlankLinesNeededAt(currLogicLine, std.blankLinesInner);
                }
            }
        }
        return logicalLinesInfo;
    }

    private void onMatchDefinition(int matchOffset) {
        if (matchOffset > 0) {
            try {
                int endLine = PySelection.getEndLineOfCurrentDeclaration(doc, offset) + 1;
                if (offset == 0 || cs[offset - 1] == '\n' || cs[offset - 1] == '\r') {
                    nextScopeStartRealLineTopLevel.push(endLine);
                } else {
                    nextScopeStartRealLineInnerLevel.push(endLine);
                }
            } catch (BadLocationException e) {
                Log.log(e);
            }

            if (decoratorState > 0) {
                decoratorState = 0;
                matchI = -1;

                // If a decorator was found before a match, delete any blank lines from
                // this declaration to the decorator.
                if (logicalLinesInfo.size() > 1) {

                    for (int reverseI = logicalLinesInfo.size() - 2; reverseI >= 0; reverseI--) {
                        LineOffsetAndInfo lineOffsetAndInfo = logicalLinesInfo.get(reverseI);
                        if (lineOffsetAndInfo.onlyWhitespacesFound) {
                            lineOffsetAndInfo.delete = true;
                        } else {
                            break;
                        }
                    }
                }
            } else {
                matchI = matchOffset;
            }
        }
    }

    private void markBlankLinesNeededAt(int currLogicLine, int blankLinesNeeded) {
        if (logicalLinesInfo.size() > 1) {
            // lst.size() == 1 means first line, so, no point in adding new line
            // lst.size() < 1 should never happen.
            int reverseI = logicalLinesInfo.size() - 2;
            // lst.size() -2 is previous line and lst.size() -1 curr line
            // so, get a comment block right before the class or def and don't
            // split it (split before the block)
            int foundAt = logicalLinesInfo.size() - 1;
            int foundAtLine = currLogicLine;
            LineOffsetAndInfo currLineOffsetAndInfo = logicalLinesInfo
                    .get(foundAt);
            while (reverseI >= 0) {
                LineOffsetAndInfo prev = logicalLinesInfo.get(reverseI);
                if (prev.infoFromLogicalLine == foundAtLine - 1) {
                    foundAtLine--;
                } else {
                    break;
                }
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
                int tempLine = logicalLinesInfo.get(reverseI).infoFromLogicalLine;
                // Ok, now, let's see if we have the proper space (the curr line is empty,
                // so, start to check spaces before it).
                for (int k = reverseI; k >= 0 && blankLinesNeeded > 0; k--) {
                    LineOffsetAndInfo info = logicalLinesInfo.get(k);
                    if (info.infoFromLogicalLine == tempLine) { // checking only subsequent lines
                        tempLine--;
                    } else {
                        break;
                    }
                    if (info.onlyWhitespacesFound) {
                        if (!info.delete) {
                            blankLinesNeeded -= (1 + info.addBlankLines);
                        }
                    } else {
                        break; // Found non-whitespace line
                    }
                }

                if (blankLinesNeeded > 0) {
                    tempLine = logicalLinesInfo.get(reverseI).infoFromLogicalLine;
                    for (int k = reverseI; k >= 0 && blankLinesNeeded > 0; k--) {
                        LineOffsetAndInfo info = logicalLinesInfo.get(k);
                        if (info.infoFromLogicalLine == tempLine) { // checking only subsequent lines
                            tempLine--;
                        } else {
                            break;
                        }
                        if (info.onlyWhitespacesFound) {
                            if (info.delete) {
                                info.delete = false;
                                blankLinesNeeded -= (1 + info.addBlankLines);
                            }
                        } else {
                            break; // Found non-whitespace line
                        }
                    }
                    if (blankLinesNeeded > 0) {
                        currLineOffsetAndInfo.addBlankLines = Math.max(currLineOffsetAndInfo.addBlankLines,
                                blankLinesNeeded);
                    }
                }
            }
        }
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
