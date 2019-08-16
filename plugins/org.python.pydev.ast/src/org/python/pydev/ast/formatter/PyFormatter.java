package org.python.pydev.ast.formatter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.PyFormatStdManageBlankLines;
import org.python.pydev.core.formatter.PyFormatStdManageBlankLines.LineOffsetAndInfo;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.pep8.BlackRunner;
import org.python.pydev.core.pep8.Pep8Runner;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_core.utils.DocUtils.EmptyLinesComputer;

public class PyFormatter {

    /**
     * This method formats a string given some standard.
     *
     * @param str the string to be formatted
     * @param std the standard to be used
     * @param parensLevel the level of the parenthesis available.
     * @return a new (formatted) string
     * @throws SyntaxErrorException
     */
    public static FastStringBuffer formatStr(String doc, FormatStd std, int parensLevel, String delimiter,
            boolean throwSyntaxError)
            throws SyntaxErrorException {
        final char[] cs = doc.toCharArray();
        FastStringBuffer buf = new FastStringBuffer();

        //Temporary buffer for some operations. Must always be cleared before it's used.
        FastStringBuffer tempBuf = new FastStringBuffer();

        ParsingUtils parsingUtils = ParsingUtils.create(cs, throwSyntaxError);
        char lastChar = '\0';
        final int length = cs.length;
        for (int i = 0; i < length; i++) {
            char c = cs[i];

            switch (c) {
                case '\'':
                case '"':
                    //ignore literals and multi-line literals, including comments...
                    i = parsingUtils.eatLiterals(buf, i, std.trimMultilineLiterals);
                    break;

                case '#':
                    i = handleComment(std, cs, buf, tempBuf, parsingUtils, i);
                    break;

                case ',':
                    i = formatForComma(std, cs, buf, i, tempBuf);
                    break;

                case '(':
                    i = formatForPar(parsingUtils, cs, i, std, buf, parensLevel + 1, delimiter, throwSyntaxError);
                    break;

                //Things to treat:
                //+, -, *, /, %
                //** // << >>
                //<, >, !=, <>, <=, >=, //=, *=, /=,
                //& ^ ~ |
                case '*':
                    //for *, we also need to treat when it's used in varargs, kwargs and list expansion
                    boolean isOperator = false;
                    for (int j = buf.length() - 1; j >= 0; j--) {
                        char localC = buf.charAt(j);
                        if (Character.isWhitespace(localC)) {
                            continue;
                        }
                        if (localC == '(' || localC == ',') {
                            //it's not an operator, but vararg. kwarg or list expansion
                        }
                        if (Character.isJavaIdentifierPart(localC)) {
                            //ok, there's a chance that it can be an operator, but we still have to check
                            //the chance that it's a wild import
                            tempBuf.clear();
                            while (Character.isJavaIdentifierPart(localC)) {
                                tempBuf.append(localC);
                                j--;
                                if (j < 0) {
                                    break; //break while
                                }
                                localC = buf.charAt(j);
                            }
                            String reversed = tempBuf.reverse().toString();
                            if (!reversed.equals("import") && !reversed.equals("lambda")) {
                                isOperator = true;
                            }
                        }
                        if (localC == '\'' || localC == ')' || localC == ']') {
                            isOperator = true;
                        }

                        //If it got here (i.e.: not whitespace), get out of the for loop.
                        break;
                    }
                    if (!isOperator) {
                        buf.append('*');
                        break;//break switch
                    }
                    //Otherwise, FALLTHROUGH

                case '+':
                case '-':

                    if (c == '-' || c == '+') { // could also be *

                        //handle exponentials correctly: e.g.: 1e-6 cannot have a space
                        tempBuf.clear();
                        boolean started = false;

                        for (int j = buf.length() - 1;; j--) {
                            if (j < 0) {
                                break;
                            }
                            char localC = buf.charAt(j);
                            if (localC == ' ' || localC == '\t') {
                                if (!started) {
                                    continue;
                                } else {
                                    break;
                                }
                            }
                            started = true;
                            if (Character.isJavaIdentifierPart(localC) || localC == '.') {
                                tempBuf.append(localC);
                            } else {
                                break;//break for
                            }
                        }
                        boolean isExponential = true;
                        String partialNumber = tempBuf.reverse().toString();
                        int partialLen = partialNumber.length();
                        if (partialLen < 2 || !Character.isDigit(partialNumber.charAt(0))) {
                            //at least 2 chars: the number and the 'e'
                            isExponential = false;
                        } else {
                            //first char checked... now, if the last is an 'e', we must leave it together no matter what
                            if (partialNumber.charAt(partialLen - 1) != 'e'
                                    && partialNumber.charAt(partialLen - 1) != 'E') {
                                isExponential = false;
                            }
                        }
                        if (isExponential) {
                            buf.rightTrimWhitespacesAndTabs();
                            buf.append(c);
                            //skip the next whitespaces from the buffer
                            int initial = i;
                            do {
                                i++;
                            } while (i < length && (c = cs[i]) == ' ' || c == '\t');
                            if (i > initial) {
                                i--;//backup 1 because we walked 1 too much.
                            }
                            break;//break switch
                        }
                        //Otherwise, FALLTHROUGH
                    }

                case '/':
                case '%':
                case '<':
                case '>':
                case '!':
                case '&':
                case '^':
                case '~':
                case '|':

                    i = handleOperator(std, cs, buf, parsingUtils, i, c);
                    c = cs[i];
                    break;

                case '@':
                    // @ can mean either a decorator or matrix multiplication,
                    // If decorator, do nothing, for matrix multiplication, '@' is an operator which
                    // may or may not be followed by an '='
                    String append = "@";
                    if (i < length - 1 && cs[i + 1] == '=') {
                        // @= found
                        i++;
                        append = "@=";
                    } else if (buf.getLastWord().trim().isEmpty()) {
                        //decorator
                        buf.append('@');
                        break;

                    }

                    while (buf.length() > 0 && buf.lastChar() == ' ') {
                        buf.deleteLast();
                    }

                    if (std.operatorsWithSpace) {
                        buf.append(' ');
                    }
                    buf.append(append);
                    //add space after
                    if (std.operatorsWithSpace) {
                        buf.append(' ');
                    }

                    i = parsingUtils.eatWhitespaces(null, i + 1);
                    break;

                //check for = and == (other cases that have an = as the operator should already be treated)
                case '=':
                    if (i < length - 1 && cs[i + 1] == '=') {
                        //if == handle as if a regular operator
                        i = handleOperator(std, cs, buf, parsingUtils, i, c);
                        c = cs[i];
                        break;
                    }

                    while (buf.length() > 0 && buf.lastChar() == ' ') {
                        buf.deleteLast();
                    }

                    boolean surroundWithSpaces = std.operatorsWithSpace;
                    if (parensLevel > 0) {
                        surroundWithSpaces = std.assignWithSpaceInsideParens;
                    }

                    //add space before
                    if (surroundWithSpaces) {
                        buf.append(' ');
                    }

                    //add the operator and the '='
                    buf.append('=');

                    //add space after
                    if (surroundWithSpaces) {
                        buf.append(' ');
                    }

                    i = parsingUtils.eatWhitespaces(null, i + 1);
                    break;

                case '\r':
                case '\n':
                    if (lastChar == ',' && std.spaceAfterComma && buf.lastChar() == ' ') {
                        buf.deleteLast();
                    }
                    if (std.trimLines) {
                        buf.rightTrimWhitespacesAndTabs();
                    }
                    buf.append(c);
                    break;

                default:
                    buf.append(c);

            }
            lastChar = c;

        }
        if (parensLevel == 0 && std.trimLines) {
            buf.rightTrimWhitespacesAndTabs();
        }

        return buf;
    }

    /**
     * Handles the case where we found a '#' in the code.
     */
    private static int handleComment(FormatStd std, char[] cs, FastStringBuffer buf, FastStringBuffer tempBuf,
            ParsingUtils parsingUtils, int i) {
        if (std.spacesBeforeComment != FormatStd.DONT_HANDLE_SPACES) {
            for (int j = i - 1; j >= 0; j--) {
                char cj = cs[j];
                if (cj == '\t' || cj == ' ') {
                    continue;
                }
                //Ok, found a non-whitespace -- if it's not a new line, we're after some
                //code, in which case we have to put the configured amount of spaces.
                if (cj != '\r' && cj != '\n') {
                    buf.rightTrimWhitespacesAndTabs();
                    buf.appendN(' ', std.spacesBeforeComment);
                }
                break;
            }
        }

        tempBuf.clear();
        i = parsingUtils.eatComments(tempBuf, i);
        if (std.trimLines) {
            String endLine = "";
            if (tempBuf.endsWith("\r\n")) {
                endLine = "\r\n";
                tempBuf.deleteLastChars(2);
            } else if (tempBuf.endsWith('\r') || tempBuf.endsWith('\n')) {
                endLine += tempBuf.lastChar();
                tempBuf.deleteLast();
            }
            tempBuf.rightTrimWhitespacesAndTabs();
            tempBuf.append(endLine);
        }

        formatComment(std, tempBuf);

        buf.append(tempBuf);
        return i;
    }

    /**
     * A comment line starting or ending with one of the following will be skipped when adding
     * spaces to the start of a comment.
     */
    private final static String[] BLOCK_COMMENT_SKIPS = new String[] {
            "###",
            "***",
            "---",
            "===",
            "+++",
            "@@@",
            "!!!",
            "~~~",
    };

    /**
     * Adds spaces after the '#' according to the configured settings. The first char of the
     * buffer passed (which is also the output) should always start with a '#'.
     */
    public static void formatComment(FormatStd std, FastStringBuffer bufWithComment) {
        if (std.spacesInStartComment > 0) {
            Assert.isTrue(bufWithComment.charAt(0) == '#');
            int len = bufWithComment.length();

            char firstCharFound = '\n';
            String bufAsString = bufWithComment.toString();
            //handle cases where the code-formatting should not take place
            if (FileUtils.isPythonShebangLine(bufAsString)) {
                return;
            }

            int spacesFound = 0;
            String remainingStringContent = "";
            for (int j = 1; j < len; j++) { //start at 1 because 0 should always be '#'
                if ((firstCharFound = bufWithComment.charAt(j)) != ' ') {
                    remainingStringContent = bufAsString.substring(j).trim();
                    break;
                }
                spacesFound += 1;
            }
            if (firstCharFound != '\r' && firstCharFound != '\n') { //Only add spaces if it wasn't an empty line.

                //handle cases where the code-formatting should not take place
                for (String s : BLOCK_COMMENT_SKIPS) {
                    if (remainingStringContent.endsWith(s) || remainingStringContent.startsWith(s)) {
                        return;
                    }
                }
                int diff = std.spacesInStartComment - spacesFound;
                if (diff > 0) {
                    bufWithComment.insertN(1, ' ', diff);
                }
            }
        }
    }

    private static Set<String> unaryWords = new HashSet<>();
    static {
        unaryWords.add("and");
        unaryWords.add("as");
        unaryWords.add("assert");
        unaryWords.add("break");
        unaryWords.add("class");
        unaryWords.add("continue");
        unaryWords.add("def");
        unaryWords.add("del");
        unaryWords.add("elif");
        unaryWords.add("else");
        unaryWords.add("except");
        unaryWords.add("exec");
        unaryWords.add("finally");
        unaryWords.add("for");
        unaryWords.add("from");
        unaryWords.add("global");
        unaryWords.add("if");
        unaryWords.add("import");
        unaryWords.add("in");
        unaryWords.add("is");
        unaryWords.add("lambda");
        unaryWords.add("nonlocal");
        unaryWords.add("not");
        unaryWords.add("or");
        unaryWords.add("pass");
        unaryWords.add("print");
        unaryWords.add("raise");
        unaryWords.add("return");
        unaryWords.add("try");
        unaryWords.add("while");
        unaryWords.add("with");
        unaryWords.add("yield");

    }

    /**
     * Handles having an operator
     *
     * @param std the coding standard to be used
     * @param cs the contents of the string
     * @param buf the buffer where the contents should be added
     * @param parsingUtils helper to get the contents
     * @param i current index
     * @param c current char
     * @return the new index after handling the operator
     */
    private static int handleOperator(FormatStd std, char[] cs, FastStringBuffer buf, ParsingUtils parsingUtils, int i,
            char c) {
        //let's discover if it's an unary operator (~ + -)
        boolean isUnaryWithContents = true;

        boolean isUnary = false;
        boolean changeWhitespacesBefore = true;
        if (c == '~' || c == '+' || c == '-') {
            //could be an unary operator...
            String trimmedLastWord = buf.getLastWord().trim();
            isUnary = trimmedLastWord.length() == 0 || unaryWords.contains(trimmedLastWord);

            if (!isUnary) {
                for (char itChar : buf.reverseIterator()) {
                    if (itChar == ' ' || itChar == '\t') {
                        continue;
                    }

                    switch (itChar) {
                        case '[':
                        case '{':
                        case '=':
                            changeWhitespacesBefore = false;

                        case '(':
                        case ':':
                            isUnaryWithContents = false;

                        case '>':
                        case '<':

                        case '-':
                        case '+':
                        case '~':

                        case '*':
                        case '/':
                        case '%':
                        case '!':
                        case '&':
                        case '^':
                        case '|':
                        case ',':
                            isUnary = true;
                    }
                    break;
                }
            } else {
                isUnaryWithContents = buf.length() > 0;
            }
        }

        //We don't want to change whitespaces before in a binary operator that is in a new line.
        for (char ch : buf.reverseIterator()) {
            if (!Character.isWhitespace(ch)) {
                break;
            }
            if (ch == '\r' || ch == '\n') {
                changeWhitespacesBefore = false;
                break;
            }
        }

        if (changeWhitespacesBefore) {
            while (buf.length() > 0 && (buf.lastChar() == ' ' || buf.lastChar() == ' ')) {
                buf.deleteLast();
            }
        }

        boolean surroundWithSpaces = std.operatorsWithSpace;

        if (changeWhitespacesBefore) {
            //add spaces before
            if (isUnaryWithContents && surroundWithSpaces) {
                buf.append(' ');
            }
        }

        char localC = c;
        char prev = '\0';
        boolean backOne = true;
        while (isOperatorPart(localC, prev)) {
            buf.append(localC);
            prev = localC;
            i++;
            if (i == cs.length) {
                break;
            }
            localC = cs[i];
            if (localC == '=') {
                //when we get to an assign, we have found a full stmt (with assign) -- e.g.: a \\=  a += a ==
                buf.append(localC);
                backOne = false;
                break;
            }
        }
        if (backOne) {
            i--;
        }

        //add space after only if it's not unary
        if (!isUnary && surroundWithSpaces) {
            buf.append(' ');
        }

        i = parsingUtils.eatWhitespaces(null, i + 1);
        return i;
    }

    /**
     * @param c the char to be checked
     * @param prev
     * @return true if the passed char is part of an operator
     */
    private static boolean isOperatorPart(char c, char prev) {
        switch (c) {
            case '+':
            case '-':
            case '~':
                if (prev == '\0') {
                    return true;
                }
                return false;

        }

        switch (c) {
            case '*':
            case '/':
            case '%':
            case '<':
            case '>':
            case '!':
            case '&':
            case '^':
            case '~':
            case '|':
            case '=':
                return true;
        }
        return false;
    }

    /**
     * Formats the contents for when a parenthesis is found (so, go until the closing parens and format it accordingly)
     * @param throwSyntaxError
     * @throws SyntaxErrorException
     */
    private static int formatForPar(final ParsingUtils parsingUtils, final char[] cs, final int i, final FormatStd std,
            final FastStringBuffer buf, final int parensLevel, final String delimiter, boolean throwSyntaxError)
            throws SyntaxErrorException {
        char c = ' ';
        FastStringBuffer locBuf = new FastStringBuffer();

        int j = i + 1;
        int start = j;
        int end = start;
        while (j < cs.length && (c = cs[j]) != ')') {

            j++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                j = parsingUtils.eatLiterals(null, j - 1, std.trimMultilineLiterals) + 1;
                end = j;

            } else if (c == '#') {
                j = parsingUtils.eatComments(null, j - 1) + 1;
                end = j;

            } else if (c == '(') { //open another par.
                if (end > start) {
                    locBuf.append(cs, start, end - start);
                    start = end;
                }
                j = formatForPar(parsingUtils, cs, j - 1, std, locBuf, parensLevel + 1, delimiter, throwSyntaxError)
                        + 1;
                start = j;

            } else {
                end = j;

            }
        }
        if (end > start) {
            locBuf.append(cs, start, end - start);
            start = end;
        }

        if (c == ')') {
            //Now, when a closing parens is found, let's see the contents of the line where that parens was found
            //and if it's only whitespaces, add all those whitespaces (to handle the following case:
            //a(a,
            //  b
            //   ) <-- we don't want to change this one.
            char c1;
            FastStringBuffer buf1 = new FastStringBuffer();

            if (locBuf.indexOf('\n') != -1 || locBuf.indexOf('\r') != -1) {
                for (int k = locBuf.length(); k > 0 && (c1 = locBuf.charAt(k - 1)) != '\n' && c1 != '\r'; k--) {
                    buf1.insert(0, c1);
                }
            }

            FastStringBuffer formatStr = formatStr(trim(locBuf).toString(), std, parensLevel, delimiter,
                    throwSyntaxError);
            FastStringBuffer formatStrBuf = trim(formatStr);

            String closing = ")";
            if (buf1.length() > 0 && PySelection.containsOnlyWhitespaces(buf1.toString())) {
                formatStrBuf.append(buf1);

            } else if (std.parametersWithSpace) {
                closing = " )";
            }

            if (std.parametersWithSpace) {
                if (formatStrBuf.length() == 0) {
                    buf.append("()");

                } else {
                    buf.append("( ");
                    buf.append(formatStrBuf);
                    buf.append(closing);
                }
            } else {
                buf.append('(');
                buf.append(formatStrBuf);
                buf.append(closing);
            }
            return j;
        } else {
            if (throwSyntaxError) {
                throw new SyntaxErrorException("No closing ')' found.");
            }
            //we found no closing parens but we finished looking already, so, let's just add anything without
            //more formatting...
            buf.append('(');
            buf.append(locBuf);
            return j;
        }
    }

    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf the buffer to be trimmed
     * @return the same buffer passed as a parameter
     */
    private static FastStringBuffer trim(FastStringBuffer locBuf) {
        while (locBuf.length() > 0 && (locBuf.firstChar() == ' ' || locBuf.firstChar() == '\t')) {
            locBuf.deleteCharAt(0);
        }
        rtrim(locBuf);
        return locBuf;
    }

    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf the buffer to be trimmed
     * @return the same buffer passed as a parameter
     */
    private static FastStringBuffer rtrim(FastStringBuffer locBuf) {
        while (locBuf.length() > 0 && (locBuf.lastChar() == ' ' || locBuf.lastChar() == '\t')) {
            locBuf.deleteLast();
        }
        return locBuf;
    }

    /**
     * When a comma is found, it's formatted accordingly (spaces added after it).
     *
     * @param std the coding standard to be used
     * @param cs the contents of the document to be formatted
     * @param buf the buffer where the comma should be added
     * @param i the current index
     * @return the new index on the original doc.
     */
    private static int formatForComma(FormatStd std, char[] cs, FastStringBuffer buf, int i,
            FastStringBuffer formatForCommaTempBuf) {
        formatForCommaTempBuf.clear();
        char c = '\0';
        while (i < cs.length - 1 && (c = cs[i + 1]) == ' ') {
            formatForCommaTempBuf.append(c);
            i++;
        }

        if (c == '#') {
            //Ok, we have a comment after a comma, let's handle it according to preferences.
            buf.append(',');
            if (std.spacesBeforeComment == FormatStd.DONT_HANDLE_SPACES) {
                //Note: other cases we won't handle here as it should be handled when the start of
                //a comment is found.
                buf.append(formatForCommaTempBuf);
            }
        } else {
            //Default: handle it as usual.
            if (std.spaceAfterComma) {
                buf.append(", ");
            } else {
                buf.append(',');
            }
        }
        return i;
    }

    public static void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps,
            FormatStd formatStd) {
        formatSelection(doc, regionsForSave, edit, ps, formatStd, Integer.MAX_VALUE);
    }

    /**
     * Formats the given selection
     * @param regionsForSave lines to be formatted (0-based).
     * @param manageBlankLinesBeforeLine we'll manage blank lines only before the passed line.
     * @see IFormatter
     */
    public static void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps,
            FormatStd formatStd, int manageBlankLinesBeforeLine) {
        //        Formatter formatter = new Formatter();
        //        formatter.formatSelection(doc, startLine, endLineIndex, edit, ps);
        Assert.isTrue(regionsForSave != null);

        switch (formatStd.formatterStyle) {
            case AUTOPEP8:
                // get a copy of formatStd to avoid being overwritten by settings
                FormatStd formatStdNew = (FormatStd) (edit != null ? edit.getFormatStd()
                        : PyFormatterPreferences.getFormatStd(null));
                // no need to remember old values, as they'll always be created from scratch
                try {
                    // assume it's a continuous region
                    if (regionsForSave.length > 0) { // at least one line selected
                        int firstSelectedLine = regionsForSave[0] + 1;
                        int lastSelectedLine = regionsForSave[regionsForSave.length - 1] + 1;
                        // hack, use global settings to pass down argument to formatStr
                        // that possibly overwrites other --range options, but that's highly unlikely
                        // autopep8 says that it accepts line-range, but then it complains in runtime
                        // so range is used instead
                        formatStdNew.autopep8Parameters += " --range " + firstSelectedLine + " " + lastSelectedLine;
                    }
                    formatAll(doc, edit, true, formatStdNew, true, false);
                } catch (SyntaxErrorException e) {
                }
                return;
            case BLACK:
                try {
                    formatAll(doc, edit, true, formatStd, true, false);
                } catch (SyntaxErrorException e1) {
                }
            case PYDEVF:
                //fallthrough
        }

        String delimiter = PySelection.getDelimiter(doc);
        IDocument formatted;
        String formattedAsStr;
        try {
            boolean allowChangingBlankLines = false;
            formattedAsStr = formatStrAutopep8OrPyDev(edit != null ? edit.getPythonNature() : null, formatStd, true,
                    doc,
                    delimiter, allowChangingBlankLines);
            formatted = new Document(formattedAsStr);
        } catch (SyntaxErrorException e) {
            return;
        } catch (MisconfigurationException e) {
            Log.log(e);
            return;
        }
        try {
            // Actually replace the formatted lines: in this formatting, lines don't change, so, this is OK :)
            // Apply the formatting from bottom to top (so that the indexes are still valid).
            int[] regionsReversed = ArrayUtils.reversedCopy(regionsForSave);

            for (int i : regionsReversed) {
                IRegion r = doc.getLineInformation(i);
                int iStart = r.getOffset();
                int iEnd = r.getOffset() + r.getLength();

                String line = PySelection.getLine(formatted, i);
                doc.replace(iStart, iEnd - iStart, line);
            }
            if (formatStd.manageBlankLines) {
                // Now, remove or add blank lines as needed.
                FastStringBuffer buf = new FastStringBuffer(formattedAsStr, 10);
                List<LineOffsetAndInfo> computed = PyFormatStdManageBlankLines
                        .computeBlankLinesAmongMethodsAndClasses(formatStd, buf, delimiter);
                Collections.reverse(computed);
                String delimTwice = delimiter + delimiter;
                String delimTimes3 = delimTwice + delimiter;
                Set<Integer> hashSet = new HashSet<>();
                EmptyLinesComputer emptyLinesComputer = new EmptyLinesComputer(doc);
                for (int i : regionsForSave) {
                    // Note: to properly deal with blank line removal, consider all blank lines as a
                    // single block (otherwise it may be really hard for the code formatter to know
                    // which of those lines should be deleted or to which of those lines a new
                    // line should be added).
                    hashSet.add(i);
                    emptyLinesComputer.addToSetEmptyLinesCloseToLine(hashSet, i);
                }

                for (LineOffsetAndInfo lineOffsetAndInfo : computed) {
                    if (!hashSet.contains(lineOffsetAndInfo.infoFromRealLine)) {
                        continue;
                    }
                    if (lineOffsetAndInfo.infoFromRealLine > manageBlankLinesBeforeLine) {
                        continue;
                    }
                    // We're going backwards to keep lines valid...
                    if (lineOffsetAndInfo.delete) {
                        String line = PySelection.getLine(doc, lineOffsetAndInfo.infoFromRealLine);
                        if (line.trim().length() == 0) {
                            // Make sure that we only delete whitespaces.
                            PySelection.deleteLine(doc, lineOffsetAndInfo.infoFromRealLine);
                        }
                    }
                    if (lineOffsetAndInfo.addBlankLines > 0) {
                        String useDelim;
                        if (lineOffsetAndInfo.addBlankLines == 1) {
                            useDelim = delimiter;
                        } else if (lineOffsetAndInfo.addBlankLines == 2) {
                            useDelim = delimTwice;
                        } else if (lineOffsetAndInfo.addBlankLines == 3) {
                            useDelim = delimTimes3;
                        } else {
                            useDelim = new FastStringBuffer().appendN(delimiter, lineOffsetAndInfo.addBlankLines)
                                    .toString();
                        }
                        doc.replace(doc.getLineInformation(lineOffsetAndInfo.infoFromRealLine).getOffset(), 0,
                                useDelim);
                    }
                }
            }

        } catch (BadLocationException | SyntaxErrorException e) {
            Log.log(e);
            return;
        }

        if (formatStd.addNewLineAtEndOfFile) {
            try {
                int len = doc.getLength();
                if (len > 0) {
                    char lastChar = doc.getChar(len - 1);
                    if (lastChar != '\r' && lastChar != '\n') {
                        doc.replace(len, 0, PySelection.getDelimiter(doc));
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

    /**
     * This method formats a string given some standard.
     *
     * @param str the string to be formatted
     * @param std the standard to be used
     * @return a new (formatted) string
     * @throws SyntaxErrorException
     */
    /*default*/public static String formatStrAutopep8OrPyDev(IPythonNature nature, IDocument doc, FormatStd std,
            String delimiter, boolean throwSyntaxError, boolean allowChangingBlankLines, File workingDir)
            throws SyntaxErrorException {
        switch (std.formatterStyle) {
            case AUTOPEP8:
                String parameters = std.autopep8Parameters;
                String formatted = Pep8Runner.runWithPep8BaseScript(doc, parameters, "autopep8.py");
                if (formatted == null) {
                    formatted = doc.get();
                }

                formatted = StringUtils.replaceNewLines(formatted, delimiter);

                return formatted;
            case BLACK:
                formatted = BlackRunner.formatWithBlack(nature, doc, std, workingDir);
                if (formatted == null) {
                    formatted = doc.get();
                }

                formatted = StringUtils.replaceNewLines(formatted, delimiter);

                return formatted;
            default:
                FastStringBuffer buf = formatStr(doc.get(), std, 0, delimiter, throwSyntaxError);
                if (allowChangingBlankLines && std.manageBlankLines) {
                    List<LineOffsetAndInfo> computed = PyFormatStdManageBlankLines
                            .computeBlankLinesAmongMethodsAndClasses(std, buf, delimiter);
                    return PyFormatStdManageBlankLines
                            .fixBlankLinesAmongMethodsAndClasses(computed, std, doc, buf, delimiter).toString();
                } else {
                    return buf.toString();
                }
        }
    }

    public static String formatStrAutopep8OrPyDev(IPythonNature nature, FormatStd formatStd, boolean throwSyntaxError,
            IDocument doc, String delimiter, boolean allowChangingBlankLines)
            throws SyntaxErrorException {
        File workingDir = null;
        if (nature != null) {
            IProject project = nature.getProject();
            if (project != null) {
                IPath location = project.getLocation();
                if (location != null) {
                    workingDir = new File(location.toOSString());
                }
            }
        }
        return formatStrAutopep8OrPyDev(nature, formatStd, throwSyntaxError,
                doc, delimiter, allowChangingBlankLines, workingDir);
    }

    /**
     * @param nature may be null (used for formatting with black).
     */
    public static String formatStrAutopep8OrPyDev(IPythonNature nature, FormatStd formatStd, boolean throwSyntaxError,
            IDocument doc, String delimiter, boolean allowChangingBlankLines, File workingDir)
            throws SyntaxErrorException {
        String formatted = formatStrAutopep8OrPyDev(nature, doc, formatStd, delimiter, throwSyntaxError,
                allowChangingBlankLines, workingDir);
        //To finish, check the end of line.
        if (formatStd.addNewLineAtEndOfFile) {
            try {
                int len = formatted.length();
                if (len > 0) {
                    char lastChar = formatted.charAt(len - 1);
                    if (lastChar != '\r' && lastChar != '\n') {
                        formatted += delimiter;
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        return formatted;
    }

    public static void formatAll(IDocument doc, IPyFormatStdProvider edit, boolean isOpenedFile, FormatStd formatStd,
            boolean throwSyntaxError, boolean allowChangingLines) throws SyntaxErrorException {
        String delimiter = PySelection.getDelimiter(doc);
        String formatted;
        try {
            formatted = formatStrAutopep8OrPyDev(edit != null ? edit.getPythonNature() : null, formatStd,
                    throwSyntaxError, doc, delimiter, allowChangingLines);
        } catch (MisconfigurationException e) {
            Log.log(e);
            return;
        }

        String contents = doc.get();
        if (contents.equals(formatted)) {
            return; //it's the same: nothing to do.
        }
        if (!isOpenedFile) {
            doc.set(formatted);
        } else {
            //let's try to apply only the differences
            TextSelectionUtils.setOnlyDifferentCode(doc, contents, formatted);
        }
    }

    /**
     * Read from stdin and write to stdout.
     */
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                throw new AssertionError("Expected either -multiple or -single in args.");
            }
            FormatStd formatStd = new FormatStd();
            formatStd.spaceAfterComma = true;
            formatStd.parametersWithSpace = false;
            formatStd.assignWithSpaceInsideParens = false;
            formatStd.operatorsWithSpace = true;
            formatStd.addNewLineAtEndOfFile = true;
            formatStd.trimLines = true;
            formatStd.trimMultilineLiterals = true;
            formatStd.spacesBeforeComment = 2;
            formatStd.spacesInStartComment = 1;
            formatStd.manageBlankLines = true;
            formatStd.blankLinesTopLevel = 2;
            formatStd.blankLinesInner = 1;

            if (args[0].equals("-multiple")) {

                // Continuously read contents from the input using an http-like protocol.
                // i.e.:
                //    Receives Content-Length: xxx\r\nCONTENTS_TO_FORMAT
                //    Writes Result: Ok|Result:SyntaxError\r\nContent-Length: xxx\r\nFORMATTED_CONTENTS
                // Note: Contents must be given in UTF-8 and len is in bytes.
                FastStringBuffer buf = new FastStringBuffer();
                while (true) {
                    String line = readLine(System.in, buf);
                    if (line == null) {
                        break; // stdin closed
                    }
                    if (!line.startsWith("Content-Length: ")) {
                        throw new AssertionError("Unexpected header (expected Content-Length: ) --> " + line);
                    }
                    String emptyLine = readLine(System.in, buf);
                    if (!emptyLine.isEmpty()) {
                        throw new AssertionError("expected Content-Length: and empty line afterwards (got: " + line
                                + " instead of empty line).");
                    }

                    // Note that we want printed not the number of bytes but number of chars.
                    String bytesToRead = line.substring("Content-Length: ".length());
                    int totalBytes = Integer.parseInt(bytesToRead);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(totalBytes);

                    byte[] tempData = new byte[Math.min(totalBytes, 8 * 1024)];
                    int remainingBytesToRead = totalBytes;
                    while (true) {
                        int bytesRead = System.in.read(tempData, 0, Math.min(tempData.length, remainingBytesToRead));
                        byteArrayOutputStream.write(tempData, 0, bytesRead);
                        remainingBytesToRead -= bytesRead;
                        if (remainingBytesToRead <= 0) {
                            break;
                        }
                    }

                    byteArrayOutputStream.flush();
                    byte[] buffer = byteArrayOutputStream.toByteArray();

                    String encoding = FileUtils.getPythonFileEncoding(buffer);
                    if (encoding == null) {
                        encoding = "utf-8";
                    }

                    String initialContent = new String(buffer, encoding);
                    Document newDoc = new Document(initialContent);

                    String delimiter = PySelection.getDelimiter(newDoc);
                    boolean allowChangingLines = true;
                    String newDocContents = "";
                    try {
                        newDocContents = PyFormatter.formatStrAutopep8OrPyDev(null, formatStd, true, newDoc, delimiter,
                                allowChangingLines);
                    } catch (SyntaxErrorException e) {
                        // Don't format: syntax is not Ok.
                        System.out.write(("Content-Length: 0\r\n").getBytes());
                        System.out.write(("Result: SyntaxError\r\n\r\n").getBytes());
                        System.out.flush();
                        continue;
                    }

                    System.out.write(("Result: Ok\r\n").getBytes());
                    byte[] bytes = newDocContents.getBytes(encoding);
                    System.out.write(("Content-Length: " + bytes.length + "\r\n\r\n").getBytes());
                    System.out.flush();
                    System.out.write(bytes);
                    System.out.flush();
                }
            } else if (args[0].equals("-single")) {
                // Read once from stdin and print result to stdout
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[32 * 1024];

                int bytesRead;
                while ((bytesRead = System.in.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] bytes = baos.toByteArray();
                String encoding = FileUtils.getPythonFileEncoding(bytes);
                if (encoding == null) {
                    encoding = "utf-8";
                }
                String initialContent = new String(bytes, encoding);

                Document newDoc = new Document(initialContent);
                String delimiter = PySelection.getDelimiter(newDoc);
                boolean allowChangingLines = true;
                String newDocContents = "";
                try {
                    newDocContents = PyFormatter.formatStrAutopep8OrPyDev(null, formatStd, true, newDoc, delimiter,
                            allowChangingLines);
                } catch (SyntaxErrorException e) {
                    // Don't format: syntax is not Ok.
                    System.exit(1);
                }
                System.out.write(newDocContents.getBytes(encoding));
                System.out.flush();
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String readLine(InputStream in, FastStringBuffer buf) throws IOException {
        buf.clear();
        while (true) {
            char c = (char) in.read();
            if (c == -1) {
                break;
            }
            if (c == '\r') {
                c = (char) in.read();
                if (c != '\n') {
                    throw new IOException("Expected line to end with \\r\\n.");
                }
                return buf.toString();
            }
            buf.append(c);
        }
        throw new AssertionError("Should not get here.");
    }
}
