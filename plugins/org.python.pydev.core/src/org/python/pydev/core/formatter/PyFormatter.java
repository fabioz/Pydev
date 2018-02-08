package org.python.pydev.core.formatter;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

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
}
