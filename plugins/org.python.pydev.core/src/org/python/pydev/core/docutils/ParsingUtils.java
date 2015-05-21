/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/07/2005
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitionerExtension2;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.shared_core.string.BaseParsingUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Helper class for parsing python code.
 *
 * @author Fabio
 */
public abstract class ParsingUtils extends BaseParsingUtils implements IPythonPartitions {

    public ParsingUtils(boolean throwSyntaxError) {
        super(throwSyntaxError);
    }

    /**
     * Class that handles char[]
     *
     * @author Fabio
     */
    private static final class FixedLenCharArrayParsingUtils extends ParsingUtils {
        private final char[] cs;
        private final int len;

        public FixedLenCharArrayParsingUtils(char[] cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs[i];
        }
    }

    /**
     * Class that handles FastStringBuffer
     *
     * @author Fabio
     */
    private static final class FixedLenFastStringBufferParsingUtils extends ParsingUtils {
        private final FastStringBuffer cs;
        private final int len;

        public FixedLenFastStringBufferParsingUtils(FastStringBuffer cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles StringBuffer
     *
     * @author Fabio
     */
    private static final class FixedLenStringBufferParsingUtils extends ParsingUtils {
        private final StringBuffer cs;
        private final int len;

        public FixedLenStringBufferParsingUtils(StringBuffer cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class FixedLenStringParsingUtils extends ParsingUtils {
        private final String cs;
        private final int len;

        public FixedLenStringParsingUtils(String cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class FixedLenIDocumentParsingUtils extends ParsingUtils {
        private final IDocument cs;
        private final int len;

        public FixedLenIDocumentParsingUtils(IDocument cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            try {
                return cs.getChar(i);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Class that handles FastStringBuffer
     *
     * @author Fabio
     */
    private static final class FastStringBufferParsingUtils extends ParsingUtils {
        private final FastStringBuffer cs;

        public FastStringBufferParsingUtils(FastStringBuffer cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.length();
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles StringBuffer
     *
     * @author Fabio
     */
    private static final class StringBufferParsingUtils extends ParsingUtils {
        private final StringBuffer cs;

        public StringBufferParsingUtils(StringBuffer cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.length();
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class IDocumentParsingUtils extends ParsingUtils {
        private final IDocument cs;

        public IDocumentParsingUtils(IDocument cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.getLength();
        }

        @Override
        public char charAt(int i) {
            try {
                return cs.getChar(i);
            } catch (BadLocationException e) {
                return '\0'; // For documents this may really happen as their len may change under the hood...
            }
        }
    }

    /**
     * Factory method to create it (and by default doesn't throw any errors).
     */
    public static ParsingUtils create(Object cs) {
        return create(cs, false);
    }

    /**
     * Factory method to create it. Object len may not be changed afterwards.
     */
    public static ParsingUtils create(Object cs, boolean throwSyntaxError, int len) {
        if (cs instanceof char[]) {
            char[] cs2 = (char[]) cs;
            return new FixedLenCharArrayParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof FastStringBuffer) {
            FastStringBuffer cs2 = (FastStringBuffer) cs;
            return new FixedLenFastStringBufferParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof StringBuffer) {
            StringBuffer cs2 = (StringBuffer) cs;
            return new FixedLenStringBufferParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof String) {
            String cs2 = (String) cs;
            return new FixedLenStringParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof IDocument) {
            IDocument cs2 = (IDocument) cs;
            return new FixedLenIDocumentParsingUtils(cs2, throwSyntaxError, len);
        }
        throw new RuntimeException("Don't know how to create instance for: " + cs.getClass());
    }

    /**
     * Factory method to create it.
     */
    public static ParsingUtils create(Object cs, boolean throwSyntaxError) {
        if (cs instanceof char[]) {
            char[] cs2 = (char[]) cs;
            return new FixedLenCharArrayParsingUtils(cs2, throwSyntaxError, cs2.length);
        }
        if (cs instanceof FastStringBuffer) {
            FastStringBuffer cs2 = (FastStringBuffer) cs;
            return new FastStringBufferParsingUtils(cs2, throwSyntaxError);
        }
        if (cs instanceof StringBuffer) {
            StringBuffer cs2 = (StringBuffer) cs;
            return new StringBufferParsingUtils(cs2, throwSyntaxError);
        }
        if (cs instanceof String) {
            String cs2 = (String) cs;
            return new FixedLenStringParsingUtils(cs2, throwSyntaxError, cs2.length());
        }
        if (cs instanceof IDocument) {
            IDocument cs2 = (IDocument) cs;
            return new IDocumentParsingUtils(cs2, throwSyntaxError);
        }
        throw new RuntimeException("Don't know how to create instance for: " + cs.getClass());
    }

    //API methods --------------------------------------------------------------------

    /**
     * @param buf used to add the comments contents (out) -- if it's null, it'll simply advance to the position and
     * return it.
     * @param i the # position
     * @return the end of the comments position (end of document or new line char)
     * @note the new line char (\r or \n) will be added as a part of the comment.
     */
    public int eatComments(FastStringBuffer buf, int i) {
        int len = len();
        char c;

        while (i < len && (c = charAt(i)) != '\n' && c != '\r') {
            if (buf != null) {
                buf.append(c);
            }
            i++;
        }

        if (i < len) {
            if (buf != null) {
                buf.append(charAt(i));
            }
        }

        return i;
    }

    /**
     * This is a special construct to try to get an import.
     */
    public int eatFromImportStatement(FastStringBuffer buf, int i) throws SyntaxErrorException {
        int len = len();
        char c = '\0';

        if (i + 5 <= len) {
            // 'from '
            if (charAt(i) == 'f' && charAt(i + 1) == 'r' && charAt(i + 2) == 'o' && charAt(i + 3) == 'm'
                    && ((c = charAt(i + 4)) == ' ' || c == '\t')) {
                i += 5;
                if (buf != null) {
                    buf.append("from");
                    buf.append(c);
                }
            } else {
                return i; //Walk nothing
            }
        } else {
            return i;
        }

        while (i < len && (c = charAt(i)) != '\n' && c != '\r') {
            if (c == '#') {
                // Just ignore any comment
                i = eatComments(null, i);

            } else if (c == '\\') {
                char c2;
                if (i + 1 < len && ((c2 = charAt(i + 1)) == '\n' || c2 == '\r')) {
                    if (buf != null) {
                        buf.append(c);
                        buf.append(c2);
                    }
                    i++;
                    if (c2 == '\r') {
                        //get \r too
                        if (i + 1 < len) {
                            c2 = charAt(i + 1);
                            if (c2 == '\n') {
                                if (buf != null) {
                                    buf.append(c2);
                                }
                                i++;
                            }
                        }
                    }
                }
                i++;

            } else if (c == '(') {

                if (buf != null) {
                    buf.append(c);
                }
                i = eatPar(i, buf);
                if (buf != null) {
                    if (i < len) {
                        buf.append(charAt(i));
                    }
                }
                i++;

            } else {
                if (buf != null) {
                    buf.append(c);
                }
                i++;
            }

        }

        return i;
    }

    /**
     * @param buf used to add the spaces (out) -- if it's null, it'll simply advance to the position and
     * return it.
     * @param i the first ' ' position
     * @return the position of the last space found
     */
    public int eatWhitespaces(FastStringBuffer buf, int i) {
        int len = len();
        char c;

        while (i < len && (c = charAt(i)) == ' ') {
            if (buf != null) {
                buf.append(c);
            }
            i++;
        }

        //go back to the last space found
        i--;

        return i;
    }

    public int eatLiteralsBackwards(FastStringBuffer buf, int i) throws SyntaxErrorException {
        //ok, current pos is ' or "
        //check if we're starting a single or multiline comment...
        char curr = charAt(i);

        if (curr != '"' && curr != '\'') {
            throw new RuntimeException("Wrong location to eat literals. Expecting ' or \" Found:" + curr);
        }

        int j = getLiteralStart(i, curr);

        if (buf != null) {
            for (int k = j; k <= i; k++) {
                buf.append(charAt(k));
            }
        }
        return j;
    }

    /**
     * Equivalent to eatLiterals(buf, startPos, false) .
     *
     * @param buf
     * @param startPos
     * @return
     * @throws SyntaxErrorException
     */
    public int eatLiterals(FastStringBuffer buf, int startPos) throws SyntaxErrorException {
        return eatLiterals(buf, startPos, false);
    }

    /**
     * Returns the index of the last character of the current string literal
     * beginning at startPos, optionally copying the contents of the literal to
     * an output buffer.
     *
     * @param buf
     *            If non-null, the contents of the literal are appended to this
     *            object.
     * @param startPos
     *            The position of the initial ' or "
     * @param rightTrimMultiline
     *            Whether to right trim the whitespace of each line in multi-
     *            line literals when appending to buf .
     * @return The position of the last ' or " character of the literal (or the
     *         end of the document).
     */
    public int eatLiterals(FastStringBuffer buf, int startPos, boolean rightTrimMultiline) throws SyntaxErrorException {
        char startChar = charAt(startPos);

        if (startChar != '"' && startChar != '\'') {
            throw new RuntimeException("Wrong location to eat literals. Expecting ' or \" ");
        }

        // Retrieves the correct end position for single- and multi-line
        // string literals.
        int endPos = getLiteralEnd(startPos, startChar);
        boolean rightTrim = rightTrimMultiline && isMultiLiteral(startPos, startChar);

        if (buf != null) {
            int lastPos = Math.min(endPos, len() - 1);
            for (int i = startPos; i <= lastPos; i++) {
                char ch = charAt(i);
                if (rightTrim && (ch == '\r' || ch == '\n')) {
                    buf.rightTrim();
                }
                buf.append(ch);
            }
        }
        return endPos;
    }

    /**
     * @param i index we are analyzing it
     * @param curr current char
     * @return the end of the multiline literal
     * @throws SyntaxErrorException
     */
    public int getLiteralStart(int i, char curr) throws SyntaxErrorException {
        boolean multi = isMultiLiteralBackwards(i, curr);

        int j;
        if (multi) {
            j = findPreviousMulti(i - 3, curr);
        } else {
            j = findPreviousSingle(i - 1, curr);
        }
        return j;
    }

    /**
     * @param i index we are analyzing it
     * @param curr current char
     * @return the end of the multiline literal
     * @throws SyntaxErrorException
     */
    public int getLiteralEnd(int i, char curr) throws SyntaxErrorException {
        boolean multi = isMultiLiteral(i, curr);

        int j;
        if (multi) {
            j = findNextMulti(i + 3, curr);
        } else {
            j = findNextSingle(i + 1, curr);
        }
        return j;
    }

    /**
     * @param i the ' or " position
     * @param buf used to add the comments contents (out)
     * @return the end of the literal position (or end of document)
     * @throws SyntaxErrorException
     */
    public int eatPar(int i, FastStringBuffer buf) throws SyntaxErrorException {
        return eatPar(i, buf, '(');
    }

    /**
     * @param i the index where we should start getting chars
     * @param buf the buffer that should be filled with the contents gotten (if null, they're ignored)
     * @return the index where the parsing stopped. It should always be the character just before the new line
     * (or before the end of the document).
     * @throws SyntaxErrorException
     */
    public int getFullFlattenedLine(int i, FastStringBuffer buf) throws SyntaxErrorException {
        char c = this.charAt(i);
        int len = len();
        boolean ignoreNextNewLine = false;
        while (i < len) {
            c = charAt(i);

            i++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                i = eatLiterals(null, i - 1) + 1;

            } else if (c == '#') {
                i = eatComments(null, i - 1);
                break;

            } else if (c == '(' || c == '[' || c == '{') { //open par.
                i = eatPar(i - 1, null, c) + 1;

            } else if (c == '\r' || c == '\n') {
                if (!ignoreNextNewLine) {
                    i--;
                    break;
                }

            } else if (c == '\\' || c == '\\') {
                ignoreNextNewLine = true;
                continue;

            } else {
                if (buf != null) {
                    buf.append(c);
                }
            }

            ignoreNextNewLine = false;
        }
        i--; //we have to do that because we passed 1 char in the beggining of the while.
        return i;
    }

    /**
     * @param buf if null, it'll simply advance without adding anything to the buffer.
     *
     * IMPORTANT: Won't add all to the buffer, only the chars found at this level (i.e.: not contents inside another [] or ()).
     * @throws SyntaxErrorException
     */
    public int eatPar(int i, FastStringBuffer buf, char par) throws SyntaxErrorException {
        char c = ' ';

        char closingPar = StringUtils.getPeer(par);

        int j = i + 1;
        int len = len();
        while (j < len && (c = charAt(j)) != closingPar) {

            j++;

            if (c == '\'' || c == '"') { //ignore comments or multiline comments...
                j = eatLiterals(null, j - 1) + 1;

            } else if (c == '#') {
                j = eatComments(null, j - 1) + 1;

            } else if (c == par) { //open another par.
                j = eatPar(j - 1, null, par) + 1;

            } else {
                if (buf != null) {
                    buf.append(c);
                }
            }
        }
        if (this.throwSyntaxError && c != closingPar) {
            throw new SyntaxErrorException();
        }
        return j;
    }

    /**
     * discover the position of the closing quote
     * @throws SyntaxErrorException
     */
    public int findNextSingle(int i, char curr) throws SyntaxErrorException {
        boolean ignoreNext = false;
        int len = len();
        while (i < len) {
            char c = charAt(i);

            if (!ignoreNext && c == curr) {
                return i;
            }

            if (!ignoreNext) {
                if (c == '\\') { //escaped quote, ignore the next char even if it is a ' or "
                    ignoreNext = true;
                }
            } else {
                ignoreNext = false;
            }

            i++;
        }
        if (throwSyntaxError) {
            throw new SyntaxErrorException();
        }
        return i;
    }

    /**
     * discover the position of the closing quote
     * @throws SyntaxErrorException
     */
    public int findPreviousSingle(int i, char curr) throws SyntaxErrorException {
        while (i >= 0) {
            char c = charAt(i);

            if (c == curr) {
                if (i > 0) {
                    if (charAt(i - 1) == '\\') {
                        //escaped
                        i--;
                        continue;
                    }
                }
                return i;
            }

            i--;
        }
        if (throwSyntaxError) {
            throw new SyntaxErrorException();
        }
        return i;
    }

    /**
     * check the end of the multiline quote
     * @throws SyntaxErrorException
     */
    public int findNextMulti(int i, char curr) throws SyntaxErrorException {
        int len = len();
        while (i + 2 < len) {
            char c = charAt(i);
            if (c == curr && charAt(i + 1) == curr && charAt(i + 2) == curr) {
                return i + 2;
            }
            i++;
            if (c == '\\') { //this is for escaped quotes
                i++;
            }
        }

        if (throwSyntaxError) {
            throw new SyntaxErrorException();
        }

        if (len < i + 2) {
            return len;
        }
        return i + 2;
    }

    /**
     * check the end of the multiline quote
     * @throws SyntaxErrorException
     */
    public int findPreviousMulti(int i, char curr) throws SyntaxErrorException {
        while (i - 2 >= 0) {
            char c = charAt(i);
            if (c == curr && charAt(i - 1) == curr && charAt(i - 2) == curr) {
                return i - 2;
            }
            i--;
        }

        if (throwSyntaxError) {
            throw new SyntaxErrorException();
        }

        //Got to the start.
        return 0;
    }

    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------
    //STATIC INTERFACES FROM NOW ON ----------------------------------------------------------------

    /**
     * @param i current position (should have a ' or ")
     * @param curr the current char (' or ")
     * @return whether we are at the end of a multi line literal or not.
     */
    public boolean isMultiLiteralBackwards(int i, char curr) {
        if (0 > i - 2) {
            return false;
        }
        if (charAt(i - 1) == curr && charAt(i - 2) == curr) {
            return true;
        }
        return false;
    }

    /**
     * @param i current position (should have a ' or ")
     * @param curr the current char (' or ")
     * @return whether we are at the start of a multi line literal or not.
     */
    public boolean isMultiLiteral(int i, char curr) {
        int len = len();
        if (len <= i + 2) {
            return false;
        }
        if (charAt(i + 1) == curr && charAt(i + 2) == curr) {
            return true;
        }
        return false;
    }

    public static void removeCommentsWhitespacesAndLiterals(FastStringBuffer buf, boolean throwSyntaxError)
            throws SyntaxErrorException {
        removeCommentsWhitespacesAndLiterals(buf, true, throwSyntaxError);
    }

    /**
     * Removes all the comments, whitespaces and literals from a FastStringBuffer (might be useful when
     * just finding matches for something).
     *
     * NOTE: the literals and the comments are changed for spaces (if we don't remove them too)
     *
     * @param buf the buffer from where things should be removed.
     * @param whitespacesToo: are you sure about the whitespaces?
     * @throws SyntaxErrorException
     */
    public static void removeCommentsWhitespacesAndLiterals(FastStringBuffer buf, boolean whitespacesToo,
            boolean throwSyntaxError) throws SyntaxErrorException {
        ParsingUtils parsingUtils = create(buf, throwSyntaxError);
        for (int i = 0; i < buf.length(); i++) { //The length can'n be extracted at this point as the buffer may change its size.
            char ch = buf.charAt(i);
            if (ch == '#') {

                int j = i;
                int len = buf.length();
                while (j < len && ch != '\n' && ch != '\r') {
                    ch = buf.charAt(j);
                    j++;
                }
                buf.delete(i, j);
                i--;

            } else if (ch == '\'' || ch == '"') {
                int j = parsingUtils.getLiteralEnd(i, ch);
                if (whitespacesToo) {
                    buf.delete(i, j + 1);
                } else {
                    for (int k = 0; i + k < j + 1; k++) {
                        buf.replace(i + k, i + k + 1, " ");
                    }
                }
            }
        }

        if (whitespacesToo) {
            buf.removeWhitespaces();
        }
    }

    public static void removeLiterals(FastStringBuffer buf, boolean throwSyntaxError) throws SyntaxErrorException {
        ParsingUtils parsingUtils = create(buf, throwSyntaxError);
        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if (ch == '#') {
                //just past through comments
                while (i < buf.length() && ch != '\n' && ch != '\r') {
                    ch = buf.charAt(i);
                    i++;
                }
            }

            if (ch == '\'' || ch == '"') {
                int j = parsingUtils.getLiteralEnd(i, ch);
                for (int k = 0; i + k < j + 1; k++) {
                    buf.replace(i + k, i + k + 1, " ");
                }
            }
        }
    }

    public static Iterator<String> getNoLiteralsOrCommentsIterator(IDocument doc) {
        return new PyDocIterator(doc);
    }

    public static void removeCommentsAndWhitespaces(FastStringBuffer buf) {

        for (int i = 0; i < buf.length(); i++) {
            char ch = buf.charAt(i);
            if (ch == '#') {

                int j = i;
                while (j < buf.length() - 1 && ch != '\n' && ch != '\r') {
                    j++;
                    ch = buf.charAt(j);
                }
                buf.delete(i, j);
            }
        }

        int length = buf.length();
        for (int i = length - 1; i >= 0; i--) {
            char ch = buf.charAt(i);
            if (Character.isWhitespace(ch)) {
                buf.deleteCharAt(i);
            }
        }
    }

    /**
     * @param initial the document
     * @param currPos the offset we're interested in
     * @return the content type of the current position
     *
     * The version with the IDocument as a parameter should be preffered, as
     * this one can be much slower (still, it is an alternative in tests or
     * other places that do not have document access), but keep in mind
     * that it may be slow.
     */
    public static String getContentType(String initial, int currPos) {
        FastStringBuffer buf = new FastStringBuffer(initial, 0);
        ParsingUtils parsingUtils = create(initial);
        String curr = PY_DEFAULT;

        for (int i = 0; i < buf.length() && i < currPos; i++) {
            char ch = buf.charAt(i);
            curr = PY_DEFAULT;

            if (ch == '#') {
                curr = PY_COMMENT;

                int j = i;
                while (j < buf.length() - 1 && ch != '\n' && ch != '\r') {
                    j++;
                    ch = buf.charAt(j);
                }
                i = j;
            }
            if (i >= currPos) {
                return curr;
            }

            if (ch == '\'' || ch == '"') {
                boolean multi = parsingUtils.isMultiLiteral(i, ch);
                if (multi) {
                    curr = PY_MULTILINE_BYTES1;
                    if (ch == '"') {
                        curr = PY_MULTILINE_BYTES2;
                    }
                } else {
                    curr = PY_SINGLELINE_BYTES1;
                    if (ch == '"') {
                        curr = PY_SINGLELINE_BYTES2;
                    }
                }
                try {
                    if (multi) {
                        i = parsingUtils.findNextMulti(i + 3, ch);
                    } else {
                        i = parsingUtils.findNextSingle(i + 1, ch);
                    }
                } catch (SyntaxErrorException e) {
                    throw new RuntimeException(e);
                }
                if (currPos < i) {
                    return curr; //found inside
                }
                if (currPos == i) {
                    if (PY_SINGLELINE_BYTES1.equals(curr) || PY_SINGLELINE_BYTES2.equals(curr)) {
                        return curr;
                    }
                }
                //if currPos == i, this means it'll go to the next partition (we always prefer open
                //partitions here, so, the last >>'<< from a string is actually treated as the start
                //of the next partition).
                curr = PY_DEFAULT;
            }
        }
        return curr;
    }

    /**
     * @param document the document we want to get info on
     * @param i the document offset we're interested in
     * @return the content type at that position (according to IPythonPartitions)
     *
     * Uses the default if the partitioner is not set in the document (for testing purposes)
     */
    public static String getContentType(IDocument document, int i) {
        IDocumentExtension3 docExtension = (IDocumentExtension3) document;
        IDocumentPartitionerExtension2 partitioner = (IDocumentPartitionerExtension2) docExtension
                .getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);

        if (partitioner != null) {
            return partitioner.getContentType(i, true);
        }
        return getContentType(document.get(), i);
    }

    public static String makePythonParseable(String code, String delimiter) {
        return makePythonParseable(code, delimiter, new FastStringBuffer());
    }

    /**
     * Ok, this method will get some code and make it suitable for putting at a shell
     * @param code the initial code we'll make parseable
     * @param delimiter the delimiter we should use
     * @return a String that can be passed to the shell
     */
    public static String makePythonParseable(String code, String delimiter, FastStringBuffer lastLine) {
        FastStringBuffer buffer = new FastStringBuffer();
        FastStringBuffer currLine = new FastStringBuffer();

        //we may have line breaks with \r\n, or only \n or \r
        boolean foundNewLine = false;
        boolean foundNewLineAtChar;
        boolean lastWasNewLine = false;

        if (lastLine.length() > 0) {
            lastWasNewLine = true;
        }

        for (int i = 0; i < code.length(); i++) {
            foundNewLineAtChar = false;
            char c = code.charAt(i);
            if (c == '\r') {
                if (i + 1 < code.length() && code.charAt(i + 1) == '\n') {
                    i++; //skip the \n
                }
                foundNewLineAtChar = true;
            } else if (c == '\n') {
                foundNewLineAtChar = true;
            }

            if (!foundNewLineAtChar) {
                if (lastWasNewLine && !Character.isWhitespace(c)) {
                    if (lastLine.length() > 0 && Character.isWhitespace(lastLine.charAt(0))) {
                        buffer.append(delimiter);
                    }
                }
                currLine.append(c);
                lastWasNewLine = false;
            } else {
                lastWasNewLine = true;
            }
            if (foundNewLineAtChar || i == code.length() - 1) {
                if (!PySelection.containsOnlyWhitespaces(currLine.toString())) {
                    buffer.append(currLine);
                    lastLine = currLine;
                    currLine = new FastStringBuffer();
                    buffer.append(delimiter);
                    foundNewLine = true;

                } else { //found a line only with whitespaces
                    currLine = new FastStringBuffer();
                }
            }
        }
        if (!foundNewLine) {
            buffer.append(delimiter);
        } else {
            if (!StringUtils.endsWith(buffer, '\r') && !StringUtils.endsWith(buffer, '\n')) {
                buffer.append(delimiter);
            }
            if (lastLine.length() > 0 && Character.isWhitespace(lastLine.charAt(0))
                    && (code.indexOf('\r') != -1 || code.indexOf('\n') != -1)) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static String removeComments(String line) {
        int i = line.indexOf('#');
        if (i != -1) {
            return line.substring(0, i);
        }
        return line;
    }

    public static boolean isStringContentType(String contentType) {
        return IPythonPartitions.PY_MULTILINE_BYTES1.equals(contentType)
                || IPythonPartitions.PY_MULTILINE_BYTES2.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_BYTES1.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_BYTES2.equals(contentType)

                || IPythonPartitions.PY_MULTILINE_UNICODE1.equals(contentType)
                || IPythonPartitions.PY_MULTILINE_UNICODE2.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_UNICODE1.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_UNICODE2.equals(contentType)

                || IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE1.equals(contentType)
                || IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE2.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE1.equals(contentType)
                || IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE2.equals(contentType)

        ;
    }

    public static boolean isCommentContentType(String contentType)
    {
        return IPythonPartitions.PY_COMMENT.equals(contentType);
    }

    public static boolean isStringPartition(IDocument document, int offset) {
        String contentType = getContentType(document, offset);
        return isStringContentType(contentType);
    }

    public static boolean isCommentPartition(IDocument document, int offset) {
        String contentType = getContentType(document, offset);
        return isCommentContentType(contentType);
    }

}
