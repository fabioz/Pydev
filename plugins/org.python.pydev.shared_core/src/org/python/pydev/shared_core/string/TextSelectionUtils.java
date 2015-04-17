/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;

public class TextSelectionUtils {

    protected IDocument doc;
    protected ITextSelection textSelection;

    /**
     * @param document the document we are using to make the selection
     * @param selection that's the actual selection. It might have an offset and a number of selected chars
     */
    public TextSelectionUtils(IDocument doc, ITextSelection selection) {
        this.doc = doc;
        this.textSelection = selection;
    }

    /**
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public TextSelectionUtils(IDocument doc, int offset) {
        this(doc, new TextSelection(doc, offset, 0));
    }

    /**
     * @return the offset of the line where the cursor is
     */
    public final int getLineOffset() {
        return getLineOffset(getCursorLine());
    }

    /**
     * @return the offset of the specified line
     */
    public final int getLineOffset(int line) {
        try {
            return getDoc().getLineInformation(line).getOffset();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @return Returns the doc.
     */
    public final IDocument getDoc() {
        return doc;
    }

    /**
     * @return Returns the cursorLine.
     */
    public final int getCursorLine() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the textSelection.
     */
    public final ITextSelection getTextSelection() {
        return textSelection;
    }

    /**
     * @return Returns the absoluteCursorOffset.
     */
    public final int getAbsoluteCursorOffset() {
        return this.getTextSelection().getOffset();
    }

    /**
     * @param src
     * @return
     */
    public static int getFirstCharPosition(String src) {
        int i = 0;
        boolean breaked = false;
        int len = src.length();
        while (i < len) {
            if (!Character.isWhitespace(src.charAt(i))) {
                i++;
                breaked = true;
                break;
            }
            i++;
        }
        if (!breaked) {
            i++;
        }
        return (i - 1);
    }

    /**
     * @return the offset mapping to the end of the line passed as parameter.
     * @throws BadLocationException
     */
    public final int getEndLineOffset(int line) throws BadLocationException {
        IRegion lineInformation = doc.getLineInformation(line);
        return lineInformation.getOffset() + lineInformation.getLength();
    }

    /**
     * @return the offset mapping to the end of the current 'end' line.
     */
    public final int getEndLineOffset() {
        IRegion endLine = getEndLine();
        return endLine.getOffset() + endLine.getLength();
    }

    /**
     * @return Returns the endLine.
     */
    public final IRegion getEndLine() {
        try {
            int endLineIndex = getEndLineIndex();
            if (endLineIndex == -1) {
                return null;
            }
            return getDoc().getLineInformation(endLineIndex);
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return Returns the endLineIndex.
     */
    public final int getEndLineIndex() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the startLine.
     */
    public IRegion getStartLine() {
        try {
            return getDoc().getLineInformation(getStartLineIndex());
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return Returns the startLineIndex.
     */
    public int getStartLineIndex() {
        return this.getTextSelection().getStartLine();
    }

    /**
     * In event of partial selection, used to select the full lines involved.
     */
    public void selectCompleteLine() {
        if (doc.getNumberOfLines() == 1) {
            this.textSelection = new TextSelection(doc, 0, doc.getLength());
            return;
        }
        IRegion endLine = getEndLine();
        IRegion startLine = getStartLine();

        this.textSelection = new TextSelection(doc, startLine.getOffset(), endLine.getOffset() + endLine.getLength()
                - startLine.getOffset());
    }

    /**
     * @return the Selected text
     */
    public String getSelectedText() {
        ITextSelection txtSel = getTextSelection();
        int start = txtSel.getOffset();
        int len = txtSel.getLength();
        try {
            return this.doc.get(start, len);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Readjust the selection so that the whole document is selected.
     *
     * @param onlyIfNothingSelected: If false, check if we already have a selection. If we
     * have a selection, it is not changed, however, if it is true, it always selects everything.
     */
    public void selectAll(boolean forceNewSelection) {
        if (!forceNewSelection) {
            if (getSelLength() > 0) {
                return;
            }
        }

        textSelection = new TextSelection(doc, 0, doc.getLength());
    }

    /**
     * @return Returns the selLength.
     */
    public int getSelLength() {
        return this.getTextSelection().getLength();
    }

    /**
     * @return Returns the selection.
     */
    public String getCursorLineContents() {
        try {
            IRegion startLine = getStartLine();
            if (startLine == null) {
                return "";
            }
            int start = startLine.getOffset();
            IRegion endLine = getEndLine();
            if (endLine == null) {
                return "";
            }
            int end = endLine.getOffset() + endLine.getLength();
            return this.doc.get(start, end - start);
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return "";
    }

    /**
     * @return the delimiter that should be used for the passed document
     */
    public static String getDelimiter(IDocument doc) {
        return TextUtilities.getDefaultLineDelimiter(doc);
    }

    /**
     * @return Returns the endLineDelim.
     */
    public String getEndLineDelim() {
        return getDelimiter(getDoc());
    }

    /**
     * @return
     * @throws BadLocationException
     */
    public char getCharAfterCurrentOffset() throws BadLocationException {
        return getDoc().getChar(getAbsoluteCursorOffset() + 1);
    }

    /**
     * @return
     * @throws BadLocationException
     */
    public char getCharAtCurrentOffset() throws BadLocationException {
        return getDoc().getChar(getAbsoluteCursorOffset());
    }

    /**
     * @return
     * @throws BadLocationException
     */
    public char getCharBeforeCurrentOffset() throws BadLocationException {
        return getDoc().getChar(getAbsoluteCursorOffset() - 1);
    }

    public static int getAbsoluteCursorOffset(IDocument doc, int line, int col) {
        try {
            IRegion offsetR = doc.getLineInformation(line);
            return offsetR.getOffset() + col;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param line 0-based
     * @param col 0-based
     * @return the absolute cursor offset in the contained document
     */
    public int getAbsoluteCursorOffset(int line, int col) {
        return getAbsoluteCursorOffset(doc, line, col);
    }

    /**
     * Changes the selection
     * @param absoluteStart this is the offset of the start of the selection
     * @param absoluteEnd this is the offset of the end of the selection
     */
    public void setSelection(int absoluteStart, int absoluteEnd) {
        this.textSelection = new TextSelection(doc, absoluteStart, absoluteEnd - absoluteStart);
    }

    /**
     * @return the current column that is selected from the cursor.
     */
    public int getCursorColumn() {
        try {
            int absoluteOffset = getAbsoluteCursorOffset();
            IRegion region = doc.getLineInformationOfOffset(absoluteOffset);
            return absoluteOffset - region.getOffset();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets current line from document.
     *
     * @return String line in String form
     */
    public String getLine() {
        return getLine(getDoc(), getCursorLine());
    }

    /**
     * Gets line from document.
     *
     * @param i Line number
     * @return String line in String form
     */
    public String getLine(int i) {
        return getLine(getDoc(), i);
    }

    /**
     * Gets line from document.
     *
     * @param i Line number
     * @return String line in String form
     */
    public static String getLine(IDocument doc, int i) {
        try {
            IRegion lineInformation = doc.getLineInformation(i);
            return doc.get(lineInformation.getOffset(), lineInformation.getLength());
        } catch (Exception e) {
            return "";
        }
    }

    public int getLineOfOffset() {
        return getLineOfOffset(this.getAbsoluteCursorOffset());
    }

    public int getLineOfOffset(int offset) {
        return getLineOfOffset(getDoc(), offset);
    }

    /**
     * @param offset the offset we want to get the line
     * @return the line of the passed offset
     */
    public static int getLineOfOffset(IDocument doc, int offset) {
        try {
            return doc.getLineOfOffset(offset);
        } catch (BadLocationException e) {
            if (offset > doc.getLength() - 1) {
                int numberOfLines = doc.getNumberOfLines();
                if (numberOfLines == 0) {
                    return 0;
                }
                return numberOfLines - 1;
            }
            return 0;
        }
    }

    /**
     * Deletes a line from the document
     * @param i
     */
    public void deleteLine(int i) {
        deleteLine(getDoc(), i);
    }

    /**
     * Deletes a line from the document
     * @param i
     */
    public static void deleteLine(IDocument doc, int i) {
        try {
            IRegion lineInformation = doc.getLineInformation(i);
            int offset = lineInformation.getOffset();

            int length = -1;

            if (doc.getNumberOfLines() > i) {
                int nextLineOffset = doc.getLineInformation(i + 1).getOffset();
                length = nextLineOffset - offset;
            } else {
                length = lineInformation.getLength();
            }

            if (length > -1) {
                doc.replace(offset, length, "");
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    public void deleteSpacesAfter(int offset) {
        try {
            final int len = countSpacesAfter(offset);
            if (len > 0) {
                doc.replace(offset, len, "");
            }
        } catch (Exception e) {
            //ignore
        }
    }

    public int countSpacesAfter(int offset) throws BadLocationException {
        if (offset >= doc.getLength()) {
            return 0;
        }

        int initial = offset;
        String next = doc.get(offset, 1);

        //don't delete 'all' that is considered whitespace (as \n and \r)
        try {
            while (next.charAt(0) == ' ' || next.charAt(0) == '\t') {
                offset++;
                next = doc.get(offset, 1);
            }
        } catch (Exception e) {
            // ignore
        }

        return offset - initial;
    }

    /**
     * Deletes the current selected text
     *
     * @throws BadLocationException
     */
    public void deleteSelection() throws BadLocationException {
        int offset = textSelection.getOffset();
        doc.replace(offset, textSelection.getLength(), "");
    }

    public void addLine(String contents, int afterLine) {
        addLine(getDoc(), getEndLineDelim(), contents, afterLine);
    }

    /**
     * Adds a line to the document.
     *
     * @param doc the document
     * @param endLineDelim the delimiter that should be used
     * @param contents what should be added (the end line delimiter may be added before or after those contents
     *  (depending on what are the current contents of the document).
     * @param afterLine the contents should be added after the line specified here.
     */
    public static void addLine(IDocument doc, String endLineDelim, String contents, int afterLine) {
        try {

            int offset = -1;
            if (doc.getNumberOfLines() > afterLine) {
                offset = doc.getLineInformation(afterLine + 1).getOffset();

            } else {
                offset = doc.getLineInformation(afterLine).getOffset();
            }

            if (doc.getNumberOfLines() - 1 == afterLine) {
                contents = endLineDelim + contents;

            }

            if (!contents.endsWith(endLineDelim)) {
                contents += endLineDelim;
            }

            if (offset >= 0) {
                doc.replace(offset, 0, contents);
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    public String getLineContentsFromCursor() throws BadLocationException {
        return getLineContentsFromCursor(getAbsoluteCursorOffset());
    }

    /**
     * @return the line where the cursor is (from the cursor position to the end of the line).
     * @throws BadLocationException
     */
    public String getLineContentsFromCursor(int offset) throws BadLocationException {
        int lineOfOffset = doc.getLineOfOffset(offset);
        IRegion lineInformation = doc.getLineInformation(lineOfOffset);

        String lineToCursor = doc.get(offset, lineInformation.getOffset() + lineInformation.getLength() - offset);
        return lineToCursor;
    }

    /**
     * @param ps
     * @return the line where the cursor is (from the beginning of the line to the cursor position).
     * @throws BadLocationException
     */
    public String getLineContentsToCursor() throws BadLocationException {
        int offset = getAbsoluteCursorOffset();
        return getLineContentsToCursor(offset);
    }

    public String getLineContentsToCursor(int offset) throws BadLocationException {
        return getLineContentsToCursor(doc, offset);
    }

    public static String getLineContentsToCursor(IDocument doc, int offset) throws BadLocationException {
        int lineOfOffset = doc.getLineOfOffset(offset);
        IRegion lineInformation = doc.getLineInformation(lineOfOffset);
        String lineToCursor = doc.get(lineInformation.getOffset(), offset - lineInformation.getOffset());
        return lineToCursor;
    }

    /**
     * Helpful for having a '|' where the cursor == | and pressing a backspace and deleting both chars.
     */
    public Tuple<String, String> getBeforeAndAfterMatchingChars(char c) {
        final int initial = getAbsoluteCursorOffset();
        int curr = initial - 1;
        IDocument doc = getDoc();
        FastStringBuffer buf = new FastStringBuffer(10);
        int length = doc.getLength();

        while (curr >= 0 && curr < length) {
            char gotten;
            try {
                gotten = doc.getChar(curr);
            } catch (BadLocationException e) {
                break;
            }
            if (gotten == c) {
                buf.append(c);
            } else {
                break;
            }
            curr--;
        }
        String before = buf.toString();
        buf.clear();
        curr = initial;

        while (curr >= 0 && curr < length) {
            char gotten;
            try {
                gotten = doc.getChar(curr);
            } catch (BadLocationException e) {
                break;
            }
            if (gotten == c) {
                buf.append(c);
            } else {
                break;
            }
            curr++;
        }
        String after = buf.toString();
        return new Tuple<String, String>(before, after);
    }

    /**
     * @return the offset mapping to the start of the current line.
     */
    public int getStartLineOffset() {
        IRegion startLine = getStartLine();
        return startLine.getOffset();
    }

    /**
     * @return the complete dotted string given the current selection and the strings after
     *
     * e.g.: if we have a text of
     * 'value = aa.bb.cc()' and 'aa' is selected, this method would return the whole dotted string ('aa.bb.cc')
     * @throws BadLocationException
     */
    public String getFullRepAfterSelection() throws BadLocationException {
        int absoluteCursorOffset = getAbsoluteCursorOffset();
        int length = doc.getLength();
        int end = absoluteCursorOffset;
        char ch = doc.getChar(end);
        while (Character.isLetterOrDigit(ch) || ch == '.') {
            end++;
            //check if we can still get some char
            if (length - 1 < end) {
                break;
            }
            ch = doc.getChar(end);
        }
        return doc.get(absoluteCursorOffset, end - absoluteCursorOffset);
    }

    /**
     * This function gets the activation token from the document given the current cursor position.
     *
     * @param document this is the document we want info on
     * @param offset this is the cursor position
     * @param getFullQualifier if true we get the full qualifier (even if it passes the current cursor location)
     * @return a tuple with the activation token and the cursor offset (may change if we need to get the full qualifier,
     *         otherwise, it is the same offset passed as a parameter).
     */
    public static Tuple<String, Integer> extractActivationToken(IDocument document, int offset,
            boolean getFullQualifier) {
        try {
            if (getFullQualifier) {
                //if we have to get the full qualifier, we'll have to walk the offset (cursor) forward
                while (offset < document.getLength()) {
                    char ch = document.getChar(offset);
                    if (Character.isJavaIdentifierPart(ch)) {
                        offset++;
                    } else {
                        break;
                    }

                }
            }
            int i = offset;

            if (i > document.getLength()) {
                return new Tuple<String, Integer>("", document.getLength()); //$NON-NLS-1$
            }

            while (i > 0) {
                char ch = document.getChar(i - 1);
                if (!Character.isJavaIdentifierPart(ch)) {
                    break;
                }
                i--;
            }

            return new Tuple<String, Integer>(document.get(i, offset - i), offset);
        } catch (BadLocationException e) {
            return new Tuple<String, Integer>("", offset); //$NON-NLS-1$
        }
    }

    /**
     * @param c
     * @param string
     */
    public static boolean containsOnly(char c, String string) {
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != c) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param string the string we care about
     * @return true if the string passed is only composed of whitespaces (or characters that
     * are regarded as whitespaces by Character.isWhitespace)
     */
    public static boolean containsOnlyWhitespaces(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isWhitespace(string.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param selection the text from where we want to get the indentation
     * @return a string representing the whitespaces and tabs befor the first char in the passed line.
     */
    public static String getIndentationFromLine(String selection) {
        int firstCharPosition = getFirstCharPosition(selection);
        return selection.substring(0, firstCharPosition);
    }

    public String getIndentationFromLine() {
        return getIndentationFromLine(getCursorLineContents());
    }

    /**
     * @param doc
     * @param region
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, IRegion region) throws BadLocationException {
        int offset = region.getOffset();
        String src = doc.get(offset, region.getLength());

        return getFirstCharPosition(src);
    }

    /**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativeLinePosition(IDocument doc, int line) throws BadLocationException {
        IRegion region;
        region = doc.getLineInformation(line);
        return getFirstCharRelativePosition(doc, region);
    }

    /**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
        region = doc.getLineInformationOfOffset(cursorOffset);
        return getFirstCharRelativePosition(doc, region);
    }

    /**
     * Returns the position of the first non whitespace char in the current line.
     * @param doc
     * @param cursorOffset
     * @return position of the first character of the line (returned as an absolute
     *            offset)
     * @throws BadLocationException
     */
    public static int getFirstCharPosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
        region = doc.getLineInformationOfOffset(cursorOffset);
        int offset = region.getOffset();
        return offset + getFirstCharRelativePosition(doc, cursorOffset);
    }

    public int getFirstCharPositionInCurrentCursorOffset() throws BadLocationException {
        return getFirstCharPosition(getDoc(), getAbsoluteCursorOffset());
    }

    /**
     * @param offset
     * @return
     */
    public boolean intersects(int offset, int len) {
        int currOffset = this.textSelection.getOffset();
        int currLen = this.textSelection.getLength();

        ///The end is after the end of the current sel
        if (offset >= currOffset + currLen) {
            return false;
        }
        if (offset + len <= currOffset) {
            return false;
        }
        return true;
    }

    /**
     * @return the current token and its initial offset for this token
     * @throws BadLocationException
     */
    public Tuple<String, Integer> getCurrToken() throws BadLocationException {
        Tuple<String, Integer> tup = extractActivationToken(doc, getAbsoluteCursorOffset(), false);
        String prefix = tup.o1;

        // ok, now, get the rest of the token, as we already have its prefix

        int start = tup.o2 - prefix.length();
        int end = start;
        while (doc.getLength() - 1 >= end) {
            char ch = doc.getChar(end);
            if (Character.isJavaIdentifierPart(ch)) {
                end++;
            } else {
                break;
            }
        }
        String post = doc.get(tup.o2, end - tup.o2);
        return new Tuple<String, Integer>(prefix + post, start);
    }

    /**
     * @return the current token and its initial offset for this token
     * @param the chars to be considered separators (note that whitespace chars are always considered separators
     * and don't need to be in this set).
     * @throws BadLocationException
     */
    public Tuple<String, Integer> getCurrToken(Set<Character> separatorChars) throws BadLocationException {
        int offset = getAbsoluteCursorOffset();
        int i = offset;

        if (i > doc.getLength()) {
            return new Tuple<String, Integer>("", doc.getLength()); //$NON-NLS-1$
        }

        while (i > 0) {
            char ch = doc.getChar(i - 1);
            if (separatorChars.contains(ch) || Character.isWhitespace(ch)) {
                break;
            }
            i--;
        }

        Tuple<String, Integer> tup = new Tuple<String, Integer>(doc.get(i, offset - i), offset);

        String prefix = tup.o1;

        // ok, now, get the rest of the token, as we already have its prefix
        int start = tup.o2 - prefix.length();
        int end = start;
        while (doc.getLength() - 1 >= end) {
            char ch = doc.getChar(end);
            if (separatorChars.contains(ch) || Character.isWhitespace(ch)) {
                break;
            }
            end++;
        }
        String post = doc.get(tup.o2, end - tup.o2);
        return new Tuple<String, Integer>(prefix + post, start);
    }

    /**
     * This function replaces all the contents in the current line before the cursor for the contents passed
     * as parameter
     */
    public void replaceLineContentsToSelection(String newContents) throws BadLocationException {
        int lineOfOffset = getDoc().getLineOfOffset(getAbsoluteCursorOffset());
        IRegion lineInformation = getDoc().getLineInformation(lineOfOffset);
        getDoc().replace(lineInformation.getOffset(), getAbsoluteCursorOffset() - lineInformation.getOffset(),
                newContents);

    }

    /**
     * @param theDoc
     * @param documentOffset
     * @return
     * @throws BadLocationException
     */
    public static int eatFuncCall(IDocument theDoc, int documentOffset) throws BadLocationException {
        String c = theDoc.get(documentOffset, 1);
        if (c.equals(")") == false) {
            throw new AssertionError("Expecting ) to eat callable. Received: " + c);
        }

        while (documentOffset > 0 && theDoc.get(documentOffset, 1).equals("(") == false) {
            documentOffset -= 1;
        }

        return documentOffset;
    }

    /**
     * Checks if the activationToken ends with some char from cs.
     */
    public static boolean endsWithSomeChar(char cs[], String activationToken) {
        for (int i = 0; i < cs.length; i++) {
            if (activationToken.endsWith(cs[i] + "")) {
                return true;
            }
        }
        return false;

    }

    public static List<Integer> getLineStartOffsets(String replacementString) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        ret.add(0);//there is always a starting one at 0

        //we may have line breaks with \r\n, or only \n or \r
        for (int i = 0; i < replacementString.length(); i++) {
            char c = replacementString.charAt(i);
            if (c == '\r') {
                i++;
                int foundAt = i;

                if (i < replacementString.length()) {
                    c = replacementString.charAt(i);
                    if (c == '\n') {
                        //                        i++;
                        foundAt = i + 1;
                    }
                }
                ret.add(foundAt);

            } else if (c == '\n') {
                ret.add(i + 1);
            }
        }

        return ret;
    }

    public static List<Integer> getLineBreakOffsets(String replacementString) {
        ArrayList<Integer> ret = new ArrayList<Integer>();

        int lineBreaks = 0;
        int ignoreNextNAt = -1;

        //we may have line breaks with \r\n, or only \n or \r
        for (int i = 0; i < replacementString.length(); i++) {
            char c = replacementString.charAt(i);
            if (c == '\r') {
                lineBreaks++;
                ret.add(i);
                ignoreNextNAt = i + 1;

            } else if (c == '\n') {
                if (ignoreNextNAt != i) {
                    ret.add(i);
                    lineBreaks++;
                }
            }
        }

        return ret;
    }

    /**
     * @return if the offset is inside the region
     */
    public static boolean isInside(int offset, IRegion region) {
        if (offset >= region.getOffset() && offset <= (region.getOffset() + region.getLength())) {
            return true;
        }
        return false;
    }

    /**
     * @return if the col is inside the initial col/len
     */
    public static boolean isInside(int col, int initialCol, int len) {
        if (col >= initialCol && col <= (initialCol + len)) {
            return true;
        }
        return false;
    }

    /**
     * @return if the region passed is composed of a single line
     */
    public static boolean endsInSameLine(IDocument document, IRegion region) {
        try {
            int startLine = document.getLineOfOffset(region.getOffset());
            int end = region.getOffset() + region.getLength();
            int endLine = document.getLineOfOffset(end);
            return startLine == endLine;
        } catch (BadLocationException e) {
            return false;
        }
    }

    /**
     * @param offset the offset we want info on
     * @return a tuple with the line, col of the passed offset in the document
     */
    public Tuple<Integer, Integer> getLineAndCol(int offset) {
        try {
            IRegion region = doc.getLineInformationOfOffset(offset);
            int line = doc.getLineOfOffset(offset);
            int col = offset - region.getOffset();
            return new Tuple<Integer, Integer>(line, col);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the contents from the document starting at the cursor line until a colon is reached.
     */
    public String getToColon() {
        FastStringBuffer buffer = new FastStringBuffer();

        int docLen = doc.getLength();
        for (int i = getLineOffset(); i < docLen; i++) {
            try {
                char c = doc.getChar(i);
                buffer.append(c);
                if (c == ':') {
                    return buffer.toString();
                }
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        return ""; //unable to find a colon
    }

    public IRegion getRegion() {
        return new Region(this.textSelection.getOffset(), this.textSelection.getLength());
    }

    public int getEndOfDocummentOffset() {
        int length = this.doc.getLength();
        return length;
    }

    public List<IRegion> searchOccurrences(String searchFor) {
        ArrayList<IRegion> lst = new ArrayList<IRegion>();
        FindReplaceDocumentAdapter adapter = new FindReplaceDocumentAdapter(this.doc);
        boolean regExSearch = false;
        boolean wholeWord = true;
        boolean caseSensitive = true;
        boolean forwardSearch = true;
        int startOffset = 0;
        try {
            while (true) {
                IRegion found = adapter.find(startOffset, searchFor, forwardSearch, caseSensitive, wholeWord,
                        regExSearch);
                if (found == null) {
                    break;
                }
                lst.add(found);
                startOffset = found.getOffset() + found.getLength();
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return lst;
    }

    /**
     * True if text ends with a newline delimiter
     */
    public static boolean endsWithNewline(IDocument document, String text) {
        String[] newlines = document.getLegalLineDelimiters();
        boolean ends = false;
        for (int i = 0; i < newlines.length; i++) {
            String delimiter = newlines[i];
            if (text.indexOf(delimiter) != -1) {
                ends = true;
            }
        }
        return ends;
    }

    /**
     * @param docContents should be == doc.get() (just optimizing if the user already did that before).
     */
    public static void setOnlyDifferentCode(IDocument doc, String docContents, String newContents) {
        String contents = docContents;
        if (contents == null) {
            contents = doc.get();
        }
        int minorLen;
        int contentsLen = contents.length();
        if (contentsLen > newContents.length()) {
            minorLen = newContents.length();
        } else {
            minorLen = contentsLen;
        }
        int applyFrom = 0;
        for (; applyFrom < minorLen; applyFrom++) {
            if (contents.charAt(applyFrom) == newContents.charAt(applyFrom)) {
                continue;
            } else {
                //different
                break;
            }
        }

        if (applyFrom >= contentsLen) {
            //Document is the same.
            return;
        }
        try {
            doc.replace(applyFrom, contentsLen - applyFrom, newContents.substring(applyFrom));
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    public Tuple<String, Integer> getCurrDottedStatement(ICharacterPairMatcher2 pairMatcher)
            throws BadLocationException {
        int absoluteCursorOffset = getAbsoluteCursorOffset();
        int start = absoluteCursorOffset;
        for (int i = absoluteCursorOffset - 1; i >= 0; i--) {
            char c = doc.getChar(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                //We're at the start now, so, let's go onwards now...
                if (StringUtils.isClosingPeer(c)) {
                    int j = pairMatcher.searchForOpeningPeer(i,
                            StringUtils.getPeer(c), c, doc);
                    if (j < 0) {
                        break;
                    }
                    i = j;
                } else {
                    break;
                }
            }
            start = i;
        }

        int len = doc.getLength();
        int end = absoluteCursorOffset;
        for (int i = absoluteCursorOffset; i < len; i++) {
            char c = doc.getChar(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                if (StringUtils.isOpeningPeer(c)) {
                    int j = pairMatcher.searchForClosingPeer(i, c,
                            StringUtils.getPeer(c), doc);
                    if (j < 0) {
                        break;
                    }
                    i = j;
                } else {
                    break;
                }
            }
            end = i + 1;
        }

        if (start != end) {
            return new Tuple<String, Integer>(doc.get(start, end - start), start);
        }

        return new Tuple<String, Integer>("", absoluteCursorOffset);
    }

    /**
     * Stop a rewrite session
     */
    public static void endWrite(IDocument doc, DocumentRewriteSession session) {
        if (doc instanceof IDocumentExtension4 && session != null) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            d.stopRewriteSession(session);
        }
    }

    /**
     * Starts a rewrite session (keep things in a single undo/redo)
     */
    public static DocumentRewriteSession startWrite(IDocument doc) {
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            return d.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        return null;
    }

    /**
     * Performs a simple sort without taking into account the actual contents of the selection (aside from lines
     * ending with '\' which are considered as a single line).
     * 
     * @param doc the document to be sorted
     * @param startLine the first line where the sort should happen
     * @param endLine the last line where the sort should happen
     */
    public void performSimpleSort(IDocument doc, int startLine, int endLine) {
        String endLineDelim = this.getEndLineDelim();
        try {
            ArrayList<String> list = new ArrayList<String>();

            StringBuffer lastLine = null;
            for (int i = startLine; i <= endLine; i++) {

                String line = getLine(doc, i);

                if (lastLine != null) {
                    int len = lastLine.length();
                    if (len > 0 && lastLine.charAt(len - 1) == '\\') {
                        lastLine.append(endLineDelim);
                        lastLine.append(line);
                    } else {
                        list.add(lastLine.toString());
                        lastLine = new StringBuffer(line);
                    }
                } else {
                    lastLine = new StringBuffer(line);
                }
            }

            if (lastLine != null) {
                list.add(lastLine.toString());
            }

            Collections.sort(list);
            StringBuffer all = new StringBuffer();
            for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
                String element = iter.next();
                all.append(element);
                if (iter.hasNext()) {
                    all.append(endLineDelim);
                }
            }

            int length = doc.getLineInformation(endLine).getLength();
            int endOffset = doc.getLineInformation(endLine).getOffset() + length;
            int startOffset = doc.getLineInformation(startLine).getOffset();

            doc.replace(startOffset, endOffset - startOffset, all.toString());

        } catch (BadLocationException e) {
            Log.log(e);
        }

    }
}
