/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: ptoofani
 * @author Fabio Zadrozny
 * Created: June 2004
 */

package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * Redone the whole class, so that the interface is better defined and no
 * duplication of information is given.
 * 
 * Now, it is just used as 'shortcuts' to document and selection settings.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public final class PySelection {

    private IDocument doc;
    private ITextSelection textSelection;
    public static final String[] DEDENT_TOKENS = new String[] { "return", "break", "continue", "pass", "raise",
    //        "yield" -- https://sourceforge.net/tracker/index.php?func=detail&aid=1807411&group_id=85796&atid=577329 (doesn't really end scope)
    //      after seeing the std lib, several cases use yield at the middle of the scope
    };

    public static final String[] CLASS_AND_FUNC_TOKENS = new String[] { "def", "class", };

    public static final String[] CLASS_TOKEN = new String[] { "class", };

    public static final String[] INDENT_TOKENS = new String[] { "if", "for", "except", "def", "class", "else", "elif",
            "while", "try", "with", "finally" };

    public static final Set<String> STATEMENT_TOKENS = new HashSet<String>();
    static {
        //Note that lambda is not here because it's usually inside other statements
        STATEMENT_TOKENS.add("assert");
        STATEMENT_TOKENS.add("break");
        STATEMENT_TOKENS.add("class");
        STATEMENT_TOKENS.add("continue");
        STATEMENT_TOKENS.add("def");
        STATEMENT_TOKENS.add("elif");
        //STATEMENT_TOKENS.add("else"); -- can be used in the construct None if True else ''
        STATEMENT_TOKENS.add("except");
        STATEMENT_TOKENS.add("finally");
        //STATEMENT_TOKENS.add("for"); -- can be used in list comprehensions
        STATEMENT_TOKENS.add("from");
        //STATEMENT_TOKENS.add("if"); -- can be used in the construct None if True else ''
        STATEMENT_TOKENS.add("import");
        STATEMENT_TOKENS.add("pass");
        STATEMENT_TOKENS.add("raise");
        STATEMENT_TOKENS.add("return");
        STATEMENT_TOKENS.add("try");
        STATEMENT_TOKENS.add("while");
        STATEMENT_TOKENS.add("with");
        STATEMENT_TOKENS.add("yield");
    };

    public static final Set<String> ALL_KEYWORD_TOKENS = new HashSet<String>();
    static {
        ALL_KEYWORD_TOKENS.add("False");
        ALL_KEYWORD_TOKENS.add("None");
        ALL_KEYWORD_TOKENS.add("True");
        ALL_KEYWORD_TOKENS.add("and");
        ALL_KEYWORD_TOKENS.add("as");
        ALL_KEYWORD_TOKENS.add("assert");
        ALL_KEYWORD_TOKENS.add("break");
        ALL_KEYWORD_TOKENS.add("class");
        ALL_KEYWORD_TOKENS.add("continue");
        ALL_KEYWORD_TOKENS.add("def");
        ALL_KEYWORD_TOKENS.add("del");
        ALL_KEYWORD_TOKENS.add("elif");
        ALL_KEYWORD_TOKENS.add("else");
        ALL_KEYWORD_TOKENS.add("except");
        ALL_KEYWORD_TOKENS.add("exec");
        ALL_KEYWORD_TOKENS.add("finally");
        ALL_KEYWORD_TOKENS.add("for");
        ALL_KEYWORD_TOKENS.add("from");
        ALL_KEYWORD_TOKENS.add("global");
        ALL_KEYWORD_TOKENS.add("if");
        ALL_KEYWORD_TOKENS.add("import");
        ALL_KEYWORD_TOKENS.add("in");
        ALL_KEYWORD_TOKENS.add("is");
        ALL_KEYWORD_TOKENS.add("lambda");
        ALL_KEYWORD_TOKENS.add("nonlocal");
        ALL_KEYWORD_TOKENS.add("not");
        ALL_KEYWORD_TOKENS.add("or");
        ALL_KEYWORD_TOKENS.add("pass");
        ALL_KEYWORD_TOKENS.add("print");
        ALL_KEYWORD_TOKENS.add("raise");
        ALL_KEYWORD_TOKENS.add("return");
        ALL_KEYWORD_TOKENS.add("self");
        ALL_KEYWORD_TOKENS.add("try");
        ALL_KEYWORD_TOKENS.add("while");
        ALL_KEYWORD_TOKENS.add("with");
        ALL_KEYWORD_TOKENS.add("yield");
    };

    /**
     * Alternate constructor for PySelection. Takes in a text editor from Eclipse.
     * 
     * @param textEditor The text editor operating in Eclipse
     */
    public PySelection(ITextEditor textEditor) {
        this(textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()), (ITextSelection) textEditor
                .getSelectionProvider().getSelection());
    }

    /**
     * @param document the document we are using to make the selection
     * @param selection that's the actual selection. It might have an offset and a number of selected chars
     */
    public PySelection(IDocument doc, ITextSelection selection) {
        this.doc = doc;
        this.textSelection = selection;
    }

    /**
     * Creates a selection from a document
     * @param doc the document to be used
     * @param line the line (starts at 0)
     * @param col the col (starts at 0)
     */
    public PySelection(IDocument doc, int line, int col) {
        this(doc, line, col, 0);
    }

    public PySelection(IDocument doc, int line, int col, int len) {
        this.doc = doc;
        this.textSelection = new TextSelection(doc, getAbsoluteCursorOffset(line, col), len);
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
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public PySelection(IDocument doc, int offset) {
        this.doc = doc;
        this.textSelection = new TextSelection(doc, offset, 0);
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
     * Creates a selection for the document, so that no characters are selected and the offset is position 0
     * @param doc the document where we are doing the selection
     */
    public PySelection(IDocument doc) {
        this(doc, 0);
    }

    /**
     * Creates a selection based on another selection.
     */
    public PySelection(PySelection base) {
        this.doc = base.doc;
        this.textSelection = new TextSelection(doc, base.getAbsoluteCursorOffset(), base.getSelLength());
    }

    /**
     * In event of partial selection, used to select the full lines involved. 
     */
    public void selectCompleteLine() {
        IRegion endLine = getEndLine();
        IRegion startLine = getStartLine();

        this.textSelection = new TextSelection(doc, startLine.getOffset(), endLine.getOffset() + endLine.getLength()
                - startLine.getOffset());
    }

    /**
     * @return true if the passed line has a from __future__ import.
     */
    public static boolean isFutureImportLine(String line) {
        List<String> split = StringUtils.split(line, ' ', '\t');
        int fromIndex = split.indexOf("from");
        int futureIndex = split.indexOf("__future__");
        boolean isFuture = fromIndex != -1 && futureIndex != -1 && futureIndex == fromIndex + 1;
        return isFuture;
    }

    /**
     * @param trimmedLine a line that's already trimmed!
     * @return true if it seems the current line is an import line (i.e.: starts with 'import' or 'from')
     */
    public static boolean isImportLine(String trimmedLine) {
        List<String> split = StringUtils.split(trimmedLine, ' ', '\t');
        if (split.size() == 0) { //nothing to see her
            return false;
        }
        String pos0 = split.get(0);
        return pos0.equals("import") || pos0.equals("from");
    }

    /**
     * @param isFutureImport if true, that means that the location found must match a from __future__ import (which
     * must be always put as the 1st import)
     *  
     * @return the line where a global import would be able to happen.
     * 
     * The 'usual' structure that we take into consideration for a py file here is:
     * 
     * #coding ...
     * 
     * '''
     * multiline comment...
     * '''
     * 
     * imports #that's what we want to find out
     * 
     * code
     */
    public int getLineAvailableForImport(boolean isFutureImport) {
        FastStringBuffer multiLineBuf = new FastStringBuffer();
        int[] firstGlobalLiteral = getFirstGlobalLiteral(multiLineBuf, 0);

        if (multiLineBuf.length() > 0 && firstGlobalLiteral[0] >= 0 && firstGlobalLiteral[1] >= 0) {
            //ok, multiline found
            int startingMultilineComment = getLineOfOffset(firstGlobalLiteral[0]);

            if (startingMultilineComment < 4) {

                //let's see if the multiline comment found is in the beginning of the document
                int lineOfOffset = getLineOfOffset(firstGlobalLiteral[1]);
                return getLineAvailableForImport(lineOfOffset + 1, isFutureImport);
            } else {

                return getLineAvailableForImport(0, isFutureImport);
            }
        } else {

            //ok, no multiline comment, let's get the first line that is not a comment
            return getLineAvailableForImport(0, isFutureImport);
        }
    }

    /**
     * @return the first line found that is not a comment.
     */
    private int getLineAvailableForImport(int startingAtLine, boolean isFutureImport) {
        int firstNonCommentLine = -1;
        int afterFirstImports = -1;

        IDocument document = getDoc();
        int lines = document.getNumberOfLines();
        ParsingUtils parsingUtils = ParsingUtils.create(document);
        for (int line = startingAtLine; line < lines; line++) {
            String str = getLine(line);
            if (str.trim().startsWith("__version__")) {
                continue;
            }
            if (str.startsWith("#")) {
                continue;
            } else {
                int i;
                if ((i = str.indexOf('#')) != -1) {
                    str = str.substring(0, i);
                }

                if (firstNonCommentLine == -1) {
                    firstNonCommentLine = line;
                }
                ImportInfo importInfo = ImportsSelection.getImportsTipperStr(str, false);
                //Don't check with trim (importInfo.importsTipperStr.trim().length()) because the string
                //will be " " in an import without a 'from'
                if (importInfo != null && importInfo.importsTipperStr != null
                        && importInfo.importsTipperStr.length() > 0) {
                    if ((i = str.indexOf('(')) != -1) {
                        //start of a multiline import
                        int lineOffset = -1;
                        try {
                            lineOffset = document.getLineOffset(line);
                        } catch (BadLocationException e1) {
                            throw new RuntimeException(e1);
                        }
                        int j;
                        try {
                            j = parsingUtils.eatPar(lineOffset + i, null);
                        } catch (SyntaxErrorException e1) {
                            throw new RuntimeException(e1);
                        }
                        try {
                            line = document.getLineOfOffset(j);
                        } catch (BadLocationException e) {
                            Log.log(e);
                        }
                    } else if (str.endsWith("\\")) {
                        while (str.endsWith("\\") && line < lines) {
                            line++;
                            str = getLine(line);
                        }
                    }
                    afterFirstImports = line + 1;
                } else if (str.trim().length() > 0) {
                    //found some non-empty, non-import, non-comment line (break it here)
                    break;
                }
            }
        }
        if (isFutureImport) {
            return firstNonCommentLine;
        }
        return afterFirstImports > firstNonCommentLine ? afterFirstImports : firstNonCommentLine;
    }

    /**
     * @param initialOffset this is the offset we should use to analyze it
     * @param buf (out) this is the comment itself
     * @return a tuple with the offset of the start and end of the first multiline comment found
     */
    public int[] getFirstGlobalLiteral(FastStringBuffer buf, int initialOffset) {
        try {
            IDocument d = getDoc();
            String strDoc = d.get(initialOffset, d.getLength() - initialOffset);

            int docLen = strDoc.length();

            if (initialOffset > docLen - 1) {
                return new int[] { -1, -1 };
            }

            char current = strDoc.charAt(initialOffset);
            ParsingUtils parsingUtils = ParsingUtils.create(strDoc);
            //for checking if it is global, it must be in the beggining of a line (must be right after a \r or \n).

            while (current != '\'' && current != '"' && initialOffset < docLen - 1) {

                //if it is inside a parenthesis, we will not take it into consideration.
                if (current == '(') {
                    initialOffset = parsingUtils.eatPar(initialOffset, buf);
                }

                initialOffset += 1;
                if (initialOffset < docLen - 1) {
                    current = strDoc.charAt(initialOffset);
                }
            }

            //either, we are at the end of the document or we found a literal
            if (initialOffset < docLen - 1) {

                if (initialOffset == 0) { //first char of the document... this is ok
                    int i = parsingUtils.eatLiterals(buf, initialOffset);
                    return new int[] { initialOffset, i };
                }

                char lastChar = strDoc.charAt(initialOffset - 1);
                //it is only global if after \r or \n
                if (lastChar == '\r' || lastChar == '\n') {
                    int i = parsingUtils.eatLiterals(buf, initialOffset);
                    return new int[] { initialOffset, i };
                }

                //ok, still not found, let's keep going
                return getFirstGlobalLiteral(buf, initialOffset + 1);
            } else {
                return new int[] { -1, -1 };

            }
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void beep(Exception e) {
        Log.log(e);
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
    }

    public static String getLineWithoutCommentsOrLiterals(String l) {
        FastStringBuffer buf = new FastStringBuffer(l, 2);
        boolean throwSyntaxError = false;
        try {
            ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false, throwSyntaxError);
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
        return buf.toString();

    }

    public String getLineWithoutCommentsOrLiterals() {
        return getLineWithoutCommentsOrLiterals(getLine());
    }

    public static String getLineWithoutLiterals(String line) {
        FastStringBuffer buf = new FastStringBuffer(line, 2);
        boolean throwSyntaxError = false;
        try {
            ParsingUtils.removeLiterals(buf, throwSyntaxError);
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
        return buf.toString();
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
     * @return the offset of the line where the cursor is
     */
    public int getLineOffset() {
        return getLineOffset(getCursorLine());
    }

    /**
     * @return the offset of the specified line
     */
    public int getLineOffset(int line) {
        try {
            return getDoc().getLineInformation(line).getOffset();
        } catch (Exception e) {
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
     * Get the current line up to where the cursor is without any comments or literals.
     */
    public String getLineContentsToCursor(boolean removeComments, boolean removeLiterals) throws BadLocationException {
        if (removeComments == false || removeLiterals == false) {
            throw new RuntimeException("Currently only accepts removing the literals and comments.");
        }
        int cursorOffset = getAbsoluteCursorOffset();

        IRegion lineInformationOfOffset = doc.getLineInformationOfOffset(cursorOffset);
        IDocumentPartitioner partitioner = PyPartitionScanner.checkPartitionScanner(doc);
        if (partitioner == null) {
            throw new RuntimeException("Partitioner not set up.");
        }

        StringBuffer buffer = new StringBuffer();
        int offset = lineInformationOfOffset.getOffset();
        int length = lineInformationOfOffset.getLength();
        for (int i = offset; i <= offset + length && i < cursorOffset; i++) {
            String contentType = partitioner.getContentType(i);
            if (contentType.equals(IPythonPartitions.PY_DEFAULT)) {
                buffer.append(doc.getChar(i));
            } else {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }

    /**
     * @param ps
     * @return the line where the cursor is (from the beggining of the line to the cursor position).
     * @throws BadLocationException
     */
    public String getLineContentsToCursor() throws BadLocationException {
        int offset = getAbsoluteCursorOffset();
        return getLineContentsToCursor(offset);
    }

    public String getLineContentsToCursor(int offset) throws BadLocationException {
        int lineOfOffset = doc.getLineOfOffset(offset);
        IRegion lineInformation = doc.getLineInformation(lineOfOffset);
        String lineToCursor = doc.get(lineInformation.getOffset(), offset - lineInformation.getOffset());
        return lineToCursor;
    }

    /**
     * Readjust the selection so that the whole document is selected.
     * 
     * @param onlyIfNothingSelected: If false, check if we already have a selection. If we
     * have a selection, it is not changed, however, if it is true, it always selects everything.
     */
    public void selectAll(boolean forceNewSelection) {
        if (!forceNewSelection) {
            if (getSelLength() > 0)
                return;
        }

        textSelection = new TextSelection(doc, 0, doc.getLength());
    }

    /**
     * @return Returns the startLineIndex.
     */
    public int getStartLineIndex() {
        return this.getTextSelection().getStartLine();
    }

    /**
     * @return Returns the endLineIndex.
     */
    public int getEndLineIndex() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the doc.
     */
    public IDocument getDoc() {
        return doc;
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
     * @return Returns the endLine.
     */
    public IRegion getEndLine() {
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
     * @return Returns the cursorLine.
     */
    public int getCursorLine() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the absoluteCursorOffset.
     */
    public int getAbsoluteCursorOffset() {
        return this.getTextSelection().getOffset();
    }

    /**
     * @return Returns the textSelection.
     */
    public ITextSelection getTextSelection() {
        return textSelection;
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
     * @return the offset mapping to the end of the line passed as parameter.
     * @throws BadLocationException 
     */
    public int getEndLineOffset(int line) throws BadLocationException {
        IRegion lineInformation = doc.getLineInformation(line);
        return lineInformation.getOffset() + lineInformation.getLength();
    }

    /**
     * @return the offset mapping to the end of the current 'end' line.
     */
    public int getEndLineOffset() {
        IRegion endLine = getEndLine();
        return endLine.getOffset() + endLine.getLength();
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

    public Tuple<String, Integer> getCurrDottedStatement() throws BadLocationException {
        int absoluteCursorOffset = getAbsoluteCursorOffset();
        int start = absoluteCursorOffset;
        for (int i = absoluteCursorOffset - 1; i >= 0; i--) {
            char c = doc.getChar(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                //We're at the start now, so, let's go onwards now...
                if (StringUtils.isClosingPeer(c)) {
                    int j = new PythonPairMatcher().searchForOpeningPeer(i, StringUtils.getPeer(c), c, doc);
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
                    int j = new PythonPairMatcher().searchForClosingPeer(i, c, StringUtils.getPeer(c), doc);
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

    public Tuple<List<String>, Integer> getInsideParentesisToks(boolean addSelf) {
        String line = getLine();
        int openParIndex = line.indexOf('(');
        if (openParIndex <= -1) { // we are in a line that does not have a parenthesis
            return null;
        }

        int lineOffset = getStartLineOffset();
        int i = lineOffset + openParIndex;

        return getInsideParentesisToks(addSelf, i, false);
    }

    public Tuple<List<String>, Integer> getInsideParentesisToks(boolean addSelf, int iLine) {
        String line = getLine(iLine);
        int openParIndex = line.indexOf('(');
        if (openParIndex <= -1) { // we are in a line that does not have a parenthesis
            return null;
        }

        int lineOffset = getLineOffset(iLine);
        int i = lineOffset + openParIndex;

        return getInsideParentesisToks(addSelf, i, false);
    }

    /**
     * This function gets the tokens inside the parenthesis that start at the current selection line
     * 
     * @param addSelf: this defines whether tokens named self should be added if it is found.
     * 
     * @param isCall: if it's a call, when we have in the parenthesis something as Call(a, (b,c)), it'll return
     * in the list as items:
     * 
     * a
     * (b,c)
     * 
     * Otherwise (in a definition), it'll return
     * 
     * a
     * b
     * c
     * 
     * @return a Tuple so that the first param is the list and the second the offset of the end of the parenthesis.
     * It may return null if no starting parenthesis was found at the current line
     */
    public Tuple<List<String>, Integer> getInsideParentesisToks(boolean addSelf, int offset, boolean isCall) {
        List<String> params = new ArrayList<String>();
        String docContents = doc.get();
        int j;
        try {
            if (isCall) {
                ParsingUtils parsingUtils = ParsingUtils.create(docContents);
                j = parsingUtils.eatPar(offset, null);
                final String insideParentesisTok = docContents.substring(offset + 1, j);
                final ParsingUtils insideParensParsingUtils = ParsingUtils.create(insideParentesisTok);
                final int len = insideParentesisTok.length();
                final FastStringBuffer buf = new FastStringBuffer(len);

                for (int i = 0; i < len; i++) {
                    char c = insideParentesisTok.charAt(i);
                    if (c == ',') {
                        String trim = buf.toString().trim();
                        if (trim.length() > 0) {
                            params.add(trim);
                        }
                        buf.clear();
                    } else {
                        switch (c) {
                            case '\'':
                            case '"':
                                j = insideParensParsingUtils.eatLiterals(null, i);
                                buf.append(insideParentesisTok.substring(i, j + 1));
                                i = j;
                                break;

                            case '{':
                            case '(':
                            case '[':
                                j = insideParensParsingUtils.eatPar(i, null, c);
                                buf.append(insideParentesisTok.substring(i, j + 1));
                                i = j;
                                break;

                            default:
                                buf.append(c);
                        }
                    }
                }
                String trim = buf.toString().trim();
                if (trim.length() > 0) {
                    params.add(trim);
                }

            } else {
                ParsingUtils parsingUtils = ParsingUtils.create(docContents);
                final FastStringBuffer buf = new FastStringBuffer();
                j = parsingUtils.eatPar(offset, buf);

                final String insideParentesisTok = buf.toString();

                StringTokenizer tokenizer = new StringTokenizer(insideParentesisTok, ",");
                while (tokenizer.hasMoreTokens()) {
                    String tok = tokenizer.nextToken();
                    String trimmed = tok.split("=")[0].trim();
                    trimmed = trimmed.replaceAll("\\(", "");
                    trimmed = trimmed.replaceAll("\\)", "");
                    if (!addSelf && trimmed.equals("self")) {
                        // don't add self...
                    } else if (trimmed.length() > 0) {
                        int colonPos;
                        if ((colonPos = trimmed.indexOf(':')) != -1) {
                            trimmed = trimmed.substring(0, colonPos);
                            trimmed = trimmed.trim();
                        }
                        if (trimmed.length() > 0) {
                            params.add(trimmed);
                        }
                    }
                }
            }
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);

        }
        return new Tuple<List<String>, Integer>(params, j);
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

    public static final String[] TOKENS_BEFORE_ELSE = new String[] { "if", "for", "except", "while", "elif" };

    public static final String[] TOKENS_BEFORE_ELIF = new String[] { "if", "elif" };

    public static final String[] TOKENS_BEFORE_EXCEPT = new String[] { "try" };

    public static final String[] TOKENS_BEFORE_FINALLY = new String[] { "try", "except" };

    /**
     * This function goes backward in the document searching for an 'if' and returns the line that has it.
     * 
     * May return null if it was not found.
     */
    public String getPreviousLineThatStartsWithToken(String[] tokens) {
        DocIterator iterator = new DocIterator(false, this, this.getCursorLine() - 1, false);
        FastStringBuffer buf = new FastStringBuffer();

        HashSet<Character> initials = new HashSet<Character>();
        for (String t : tokens) {
            if (t.length() > 0) {
                initials.add(t.charAt(0));
            }
        }

        int indentMustBeHigherThan = -1;
        int currLineIndent = -1;
        int skipLinesHigherThan = Integer.MAX_VALUE;

        while (iterator.hasNext()) {
            String line = (String) iterator.next();
            String trimmed = line.trim();
            int len = trimmed.length();
            int lastReturnedLine = iterator.getLastReturnedLine();
            if (lastReturnedLine > skipLinesHigherThan) {
                continue;
            }

            if (len > 0) {
                //Fast way out of a line...
                char c0 = trimmed.charAt(0);

                if (currLineIndent == 0) {
                    //actually, at this point it's from the previous line...

                    //If the indent expected is == 0, if the indent wasn't found on the first match, it's not possible
                    //to get a lower match!
                    return null;
                }
                currLineIndent = getFirstCharPosition(line);
                if (indentMustBeHigherThan == -1) {
                    if (c0 != '#') {
                        //ignore only-comment lines...
                        boolean validIndentLine = true;
                        Tuple<Character, Integer> found = null;
                        for (char c : StringUtils.CLOSING_BRACKETS) {
                            int i = line.lastIndexOf(c);
                            if (found == null || found.o2 < i) {
                                found = new Tuple<Character, Integer>(c, i);
                            }
                        }
                        if (found != null) {
                            PythonPairMatcher matcher = new PythonPairMatcher();
                            int openingPeerOffset = matcher.searchForOpeningPeer(this.getLineOffset(lastReturnedLine)
                                    + found.o2, StringUtils.getPeer(found.o1), found.o1, this.getDoc());
                            if (openingPeerOffset >= 0) {
                                int lineOfOffset = getLineOfOffset(openingPeerOffset);
                                if (lineOfOffset != lastReturnedLine) {
                                    skipLinesHigherThan = lineOfOffset;
                                    validIndentLine = false;
                                }
                            }
                        }

                        if (validIndentLine) {
                            indentMustBeHigherThan = currLineIndent;
                        } else {
                            currLineIndent = -1;
                            continue;
                        }
                    }

                } else {
                    if (indentMustBeHigherThan <= currLineIndent) {
                        continue;
                    }
                }

                if (!initials.contains(c0)) {
                    continue;
                }

                buf.clear();
                buf.append(c0);
            }

            for (int i = 1; i < len; i++) {
                char c = trimmed.charAt(i);
                if (Character.isJavaIdentifierPart(c)) {
                    buf.append(c);
                } else {
                    break;
                }
            }
            String firstWord = buf.toString();
            for (String prefix : tokens) {
                if (firstWord.equals(prefix)) {
                    return line;
                }
            }
        }
        return null;
    }

    public LineStartingScope getPreviousLineThatStartsScope() {
        return getPreviousLineThatStartsScope(PySelection.INDENT_TOKENS, true, Integer.MAX_VALUE);
    }

    public LineStartingScope getNextLineThatStartsScope() {
        return getNextLineThatStartsScope(PySelection.INDENT_TOKENS, true, Integer.MAX_VALUE);
    }

    public LineStartingScope getPreviousLineThatStartsScope(String[] indentTokens, boolean considerCurrentLine,
            int mustHaveIndentLowerThan) {
        int lineToStart = -1;
        if (!considerCurrentLine) {
            lineToStart = getCursorLine() - 1;
        }
        return getPreviousLineThatStartsScope(indentTokens, lineToStart, mustHaveIndentLowerThan);
    }

    public LineStartingScope getNextLineThatStartsScope(String[] indentTokens, boolean considerCurrentLine,
            int mustHaveIndentLowerThan) {
        int lineToStart = -1;
        if (!considerCurrentLine) {
            lineToStart = getCursorLine() - 1;
        }
        return getNextLineThatStartsScope(indentTokens, lineToStart, mustHaveIndentLowerThan);
    }

    public LineStartingScope getPreviousLineThatStartsScope(int lineToStart) {
        return getPreviousLineThatStartsScope(PySelection.INDENT_TOKENS, lineToStart, Integer.MAX_VALUE);
    }

    public static class LineStartingScope {

        public final String lineStartingScope;
        public final String lineWithDedentWhileLookingScope;
        public final String lineWithLowestIndent;
        public final int iLineStartingScope;

        public LineStartingScope(String lineStartingScope, String lineWithDedentWhileLookingScope,
                String lineWithLowestIndent, int iLineStartingScope) {
            this.lineStartingScope = lineStartingScope;
            this.lineWithDedentWhileLookingScope = lineWithDedentWhileLookingScope;
            this.lineWithLowestIndent = lineWithLowestIndent;
            this.iLineStartingScope = iLineStartingScope;
        }
    }

    public LineStartingScope getNextLineThatStartsScope(String[] indentTokens, int lineToStart,
            int mustHaveIndentLowerThan) {
        return getLineThatStartsScope(true, indentTokens, lineToStart, mustHaveIndentLowerThan);
    }

    public LineStartingScope getPreviousLineThatStartsScope(String[] indentTokens, int lineToStart,
            int mustHaveIndentLowerThan) {
        return getLineThatStartsScope(false, indentTokens, lineToStart, mustHaveIndentLowerThan);
    }

    /**
     * @param lineToStart: if -1, it'll start at the current line.
     * 
     * @return a tuple with:
     * - the line that starts the new scope 
     * - a String with the line where some dedent token was found while looking for that scope.
     * - a string with the lowest indent (null if none was found)
     */
    public LineStartingScope getLineThatStartsScope(boolean forward, String[] indentTokens, int lineToStart,
            int mustHaveIndentLowerThan) {
        final DocIterator iterator;
        if (lineToStart == -1) {
            iterator = new DocIterator(forward, this);
        } else {
            iterator = new DocIterator(forward, this, lineToStart, false);
        }

        String foundDedent = null;
        String lowestStr = null;

        while (iterator.hasNext()) {
            if (mustHaveIndentLowerThan == 0) {
                return null; //we won't find any indent lower than that.
            }
            String line = (String) iterator.next();
            String trimmed = line.trim();

            if (trimmed.startsWith("#")) {
                continue;
            }

            for (String dedent : indentTokens) {
                if (trimmed.startsWith(dedent)) {
                    if (isCompleteToken(trimmed, dedent)) {
                        if (PySelection.getFirstCharPosition(line) < mustHaveIndentLowerThan) {
                            return new LineStartingScope(line, foundDedent, lowestStr, iterator.getLastReturnedLine());
                        } else {
                            break; //we won't find any other because the indent is already wrong.
                        }
                    }
                }
            }
            //we have to check for the first condition (if a dedent is found, but we already found 
            //one with a first char, the dedent should not be taken into consideration... and vice-versa).
            if (lowestStr == null && foundDedent == null && startsWithDedentToken(trimmed)) {
                foundDedent = line;

            } else if (foundDedent == null && trimmed.length() > 0) {
                int firstCharPosition = getFirstCharPosition(line);
                if (firstCharPosition < mustHaveIndentLowerThan) {
                    mustHaveIndentLowerThan = firstCharPosition;
                    lowestStr = line;
                }
            }

        }
        return null;
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

    public static class ActivationTokenAndQual {
        public ActivationTokenAndQual(String activationToken, String qualifier, boolean changedForCalltip,
                boolean alreadyHasParams, boolean isInMethodKeywordParam, int offsetForKeywordParam, int calltipOffset) {
            this.activationToken = activationToken;
            this.qualifier = qualifier;
            this.changedForCalltip = changedForCalltip;
            this.alreadyHasParams = alreadyHasParams;
            this.isInMethodKeywordParam = isInMethodKeywordParam;
            this.offsetForKeywordParam = offsetForKeywordParam;
            this.calltipOffset = calltipOffset;
        }

        public final String activationToken;
        public final String qualifier;
        public final boolean changedForCalltip;
        public final boolean alreadyHasParams;
        public final boolean isInMethodKeywordParam;
        public final int offsetForKeywordParam; //only set when isInMethodKeywordParam == true
        public final int calltipOffset; //this is where the parameters start
    }

    /**
     * Shortcut
     */
    public String[] getActivationTokenAndQual(boolean getFullQualifier) {
        return getActivationTokenAndQual(doc, getAbsoluteCursorOffset(), getFullQualifier);
    }

    /**
     * Shortcut
     */
    public ActivationTokenAndQual getActivationTokenAndQual(boolean getFullQualifier, boolean handleForCalltips) {
        return getActivationTokenAndQual(doc, getAbsoluteCursorOffset(), getFullQualifier, handleForCalltips);
    }

    /**
     * Shortcut
     */
    public static String[] getActivationTokenAndQual(IDocument theDoc, int documentOffset, boolean getFullQualifier) {
        ActivationTokenAndQual ret = getActivationTokenAndQual(theDoc, documentOffset, getFullQualifier, false);
        return new String[] { ret.activationToken, ret.qualifier }; //will never be changed for the calltip, as we didn't request it
    }

    /**
     * Returns the activation token.
     * 
     * @param documentOffset the current cursor offset (we may have to change it if getFullQualifier is true)
     * @param handleForCalltips if true, it will take into account that we may be looking for the activation token and
     * qualifier for a calltip, in which case we should return the activation token and qualifier before a parenthesis (if we're
     * just after a '(' or ',' ).
     * 
     * @return the activation token and the qualifier.
     */
    public static ActivationTokenAndQual getActivationTokenAndQual(IDocument doc, int documentOffset,
            boolean getFullQualifier, boolean handleForCalltips) {
        boolean changedForCalltip = false;
        boolean alreadyHasParams = false; //only useful if we're in a calltip
        int parOffset = -1;
        boolean isInMethodKeywordParam = false;
        int offsetForKeywordParam = -1;

        int foundCalltipOffset = -1;
        if (handleForCalltips) {
            int calltipOffset = documentOffset - 1;
            //ok, in this case, we have to check if we're just after a ( or ,
            if (calltipOffset > 0 && calltipOffset < doc.getLength()) {
                try {
                    char c = doc.getChar(calltipOffset);
                    while (Character.isWhitespace(c) && calltipOffset > 0) {
                        calltipOffset--;
                        c = doc.getChar(calltipOffset);
                    }
                    if (c == '(' || c == ',') {
                        //ok, we're just after a parenthesis or comma, so, we have to get the
                        //activation token and qualifier as if we were just before the last parenthesis
                        //(that is, if we're in a function call and not inside a list, string or dict declaration)
                        parOffset = calltipOffset;
                        calltipOffset = getBeforeParentesisCall(doc, calltipOffset);

                        if (calltipOffset != -1) {
                            documentOffset = calltipOffset;
                            changedForCalltip = true;
                            foundCalltipOffset = calculateProperCalltipOffset(doc, calltipOffset);
                        }
                    } else {
                        c = doc.getChar(calltipOffset);
                        while ((Character.isJavaIdentifierPart(c) || Character.isWhitespace(c)) && calltipOffset > 0) {
                            calltipOffset--;
                            c = doc.getChar(calltipOffset);
                        }
                        if (c == '(' || c == ',') {
                            calltipOffset = getBeforeParentesisCall(doc, calltipOffset);
                            if (calltipOffset != -1) {
                                offsetForKeywordParam = calltipOffset;
                                isInMethodKeywordParam = true;
                                foundCalltipOffset = calculateProperCalltipOffset(doc, calltipOffset);
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        if (parOffset != -1) {
            //ok, let's see if there's something inside the parenthesis
            try {
                char c = doc.getChar(parOffset);
                if (c == '(') { //only do it 
                    parOffset++;
                    while (parOffset < doc.getLength()) {
                        c = doc.getChar(parOffset);
                        if (c == ')') {
                            break; //finished the parenthesis
                        }

                        if (!Character.isWhitespace(c)) {
                            alreadyHasParams = true;
                            break;
                        }
                        parOffset++;
                    }
                } else {
                    //we're after a comma, so, there surely is some parameter already
                    alreadyHasParams = true;
                }
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }

        Tuple<String, Integer> tupPrefix = extractActivationToken(doc, documentOffset, getFullQualifier);

        if (getFullQualifier == true) {
            //may have changed
            documentOffset = tupPrefix.o2;
        }

        String activationToken = tupPrefix.o1;
        documentOffset = documentOffset - activationToken.length() - 1;

        try {
            while (documentOffset >= 0 && documentOffset < doc.getLength() && doc.get(documentOffset, 1).equals(".")) {
                String tok = extractActivationToken(doc, documentOffset, false).o1;

                if (documentOffset == 0) {
                    break;
                }

                String c = doc.get(documentOffset - 1, 1);

                if (c.equals("]")) {
                    // consume [.*] 
                    int docOff = documentOffset;
                    while (docOff > 0 && doc.get(docOff, 1).equals("[") == false) {
                        docOff -= 1;
                    }
                    // get activation token for the accessed list derivative
                    tok = extractActivationToken(doc, docOff, false).o1;

                    if (tok.length() > 0) {
                        // see handling of function call below
                        // this won't work for pure lists at the moment
                        activationToken = tok + ".__getitem__()." + activationToken;
                        documentOffset = docOff - tok.length() - 1;

                    } else {
                        // (old) fall-back handling
                        activationToken = "list." + activationToken;
                    }
                    break;

                } else if (c.equals("}")) {
                    activationToken = "dict." + activationToken;
                    break;

                } else if (c.equals("'") || c.equals("\"")) {
                    activationToken = "str." + activationToken;
                    break;

                } else if (c.equals(")")) {
                    documentOffset = eatFuncCall(doc, documentOffset - 1);
                    tok = extractActivationToken(doc, documentOffset, false).o1;
                    activationToken = tok + "()." + activationToken;
                    documentOffset = documentOffset - tok.length() - 1;

                } else if (tok.length() > 0) {
                    activationToken = tok + "." + activationToken;
                    documentOffset = documentOffset - tok.length() - 1;

                } else {
                    break;
                }

            }
        } catch (BadLocationException e) {
            Log.log("documentOffset " + documentOffset + "\n" + "theDoc.getLength() " + doc.getLength(), e);
        }

        String qualifier = "";
        //we complete on '.' and '('.
        //' ' gets globals
        //and any other char gets globals on token and templates.

        //we have to get the qualifier. e.g. bla.foo = foo is the qualifier.
        if (activationToken.indexOf('.') != -1) {
            while (endsWithSomeChar(new char[] { '.', '[' }, activationToken) == false && activationToken.length() > 0) {

                qualifier = activationToken.charAt(activationToken.length() - 1) + qualifier;
                activationToken = activationToken.substring(0, activationToken.length() - 1);
            }
        } else { //everything is a part of the qualifier.
            qualifier = activationToken.trim();
            activationToken = "";
        }
        return new ActivationTokenAndQual(activationToken, qualifier, changedForCalltip, alreadyHasParams,
                isInMethodKeywordParam, offsetForKeywordParam, foundCalltipOffset);
    }

    private static int calculateProperCalltipOffset(IDocument doc, int calltipOffset) {
        try {
            char c = doc.getChar(calltipOffset);
            while (c != '(') {
                calltipOffset++;
                c = doc.getChar(calltipOffset);
            }
            calltipOffset++; //right after the parenthesis
            return calltipOffset;
        } catch (BadLocationException e) {
        }
        return -1;
    }

    /**
     * This function will look for a the offset of a method call before the current offset
     * 
     * @param doc: an IDocument, String, StringBuffer or char[]
     * @param calltipOffset the offset we should start looking for it
     * @return the offset that points the location just after the activation token and qualifier.
     * 
     * @throws BadLocationException 
     */
    public static int getBeforeParentesisCall(Object doc, int calltipOffset) {
        ParsingUtils parsingUtils = ParsingUtils.create(doc);
        char c = parsingUtils.charAt(calltipOffset);

        while (calltipOffset > 0 && c != '(') {
            calltipOffset--;
            c = parsingUtils.charAt(calltipOffset);
        }
        if (c == '(') {
            while (calltipOffset > 0 && Character.isWhitespace(c)) {
                calltipOffset--;
                c = parsingUtils.charAt(calltipOffset);
            }
            return calltipOffset;
        }
        return -1;
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
    public static Tuple<String, Integer> extractActivationToken(IDocument document, int offset, boolean getFullQualifier) {
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

            if (i > document.getLength())
                return new Tuple<String, Integer>("", document.getLength()); //$NON-NLS-1$

            while (i > 0) {
                char ch = document.getChar(i - 1);
                if (!Character.isJavaIdentifierPart(ch))
                    break;
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
     * @param src
     * @return
     */
    public static int getFirstCharPosition(String src) {
        int i = 0;
        boolean breaked = false;
        while (i < src.length()) {
            if (Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t') {
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

    /**
     * @return true if this line starts with a dedent token (the passed string should be already trimmed)
     */
    public static boolean startsWithDedentToken(String trimmedLine) {
        for (String dedent : PySelection.DEDENT_TOKENS) {
            if (trimmedLine.startsWith(dedent)) {
                return isCompleteToken(trimmedLine, dedent);
            }
        }
        return false;
    }

    /**
     * @return true if this line starts with an indent token (the passed string should be already trimmed)
     */
    public static boolean startsWithIndentToken(String trimmedLine) {
        for (String dedent : PySelection.INDENT_TOKENS) {
            if (trimmedLine.startsWith(dedent)) {
                return isCompleteToken(trimmedLine, dedent);
            }
        }
        return false;
    }

    private static boolean isCompleteToken(String trimmedLine, String dedent) {
        if (dedent.length() < trimmedLine.length()) {
            char afterToken = trimmedLine.charAt(dedent.length());
            if (afterToken == ' ' || afterToken == ':' || afterToken == ';' || afterToken == '(') {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Class to help iterating through the document
     */
    public static class DocIterator implements Iterator<String> {
        private int startingLine;
        private boolean forward;
        private boolean isFirst = true;
        private int numberOfLines;
        private int lastReturnedLine = -1;
        private PySelection ps;

        public DocIterator(boolean forward, PySelection ps) {
            this(forward, ps, ps.getCursorLine(), true);
        }

        public DocIterator(boolean forward, PySelection ps, int startingLine, boolean considerFirst) {
            this.startingLine = startingLine;
            this.forward = forward;
            numberOfLines = ps.getDoc().getNumberOfLines();
            this.ps = ps;
            if (!considerFirst) {
                isFirst = false;
            }
        }

        public int getCurrentLine() {
            return startingLine;
        }

        public boolean hasNext() {
            if (forward) {
                return startingLine < numberOfLines;
            } else {
                return startingLine >= 0;
            }
        }

        /**
         * Note that the first thing it returns is the lineContents to cursor (and only after that
         * does it return from the full line -- if it is iterating backwards).
         */
        public String next() {
            try {
                String line;
                if (forward) {
                    line = ps.getLine(startingLine);
                    lastReturnedLine = startingLine;
                    startingLine++;
                } else {
                    if (isFirst) {
                        line = ps.getLineContentsToCursor();
                        isFirst = false;
                    } else {
                        line = ps.getLine(startingLine);
                    }
                    lastReturnedLine = startingLine;
                    startingLine--;
                }
                return line;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void remove() {
            throw new RuntimeException("Remove not implemented.");
        }

        public int getLastReturnedLine() {
            return lastReturnedLine;
        }
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

    /**
     * @param matchOnlyComplete if true matches only if a complete signature is found. If false,
     * matches even if only the 'def' and name are available.
     */
    public boolean isInFunctionLine(boolean matchOnlyComplete) {
        String line;
        if (!matchOnlyComplete) {
            //does not requires colon
            line = this.getLine();
        } else {
            //requires colon
            line = getToColon();
        }
        return matchesFunctionLine(line);
    }

    public static boolean matchesFunctionLine(String line) {
        return FunctionPattern.matcher(line.trim()).matches();
    }

    public static boolean isIdentifier(String str) {
        return IdentifierPattern.matcher(str).matches();
    }

    public boolean isInClassLine() {
        String line = this.getLine().trim();
        return matchesClassLine(line);
    }

    public static boolean matchesClassLine(String line) {
        return ClassPattern.matcher(line).matches();
    }

    //spaces* 'def' space+ identifier space* ( (space|char|.|,|=|*|(|)|'|")* ):
    private static final Pattern FunctionPattern = Pattern.compile("\\s*def\\s+\\w*.*", Pattern.DOTALL);

    //spaces* 'class' space+ identifier space* (? (.|char|space |,)* )?
    private static final Pattern ClassPattern = Pattern.compile("\\s*class\\s+\\w*.*", Pattern.DOTALL);

    private static final Pattern IdentifierPattern = Pattern.compile("\\w*");

    public static boolean isCommentLine(String line) {
        for (int j = 0; j < line.length(); j++) {
            char c = line.charAt(j);
            if (c == '#') {
                //ok, it starts with # (so, it is a comment)
                return true;
            } else if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return false;
    }

    public static int DECLARATION_NONE = 0;
    public static int DECLARATION_CLASS = 1;
    public static int DECLARATION_METHOD = 2;

    /**
     * @return whether the current selection is on the ClassName or Function name context
     * (just after the 'class' or 'def' tokens)
     */
    public int isInDeclarationLine() {
        try {
            String contents = getLineContentsToCursor();
            StringTokenizer strTok = new StringTokenizer(contents);
            if (strTok.hasMoreTokens()) {
                String tok = strTok.nextToken();
                int decl = DECLARATION_NONE;
                if (tok.equals("class")) {
                    decl = DECLARATION_CLASS;
                } else if (tok.equals("def")) {
                    decl = DECLARATION_METHOD;
                }
                if (decl != DECLARATION_NONE) {

                    //ok, we're in a class or def line... so, if we find a '(' or ':', we're not in the declaration... 
                    //(otherwise, we're in it)
                    while (strTok.hasMoreTokens()) {
                        tok = strTok.nextToken();
                        if (tok.indexOf('(') != -1 || tok.indexOf(':') != -1) {
                            return DECLARATION_NONE;
                        }
                    }
                    return decl;
                }
            }
        } catch (BadLocationException e) {
        }
        return DECLARATION_NONE;
    }

    public IRegion getRegion() {
        return new Region(this.textSelection.getOffset(), this.textSelection.getLength());
    }

    public int getEndOfDocummentOffset() {
        int length = this.doc.getLength();
        return length;
    }

    /**
     * @param currentOffset the current offset should be at the '(' or at a space before it (if we are at any other
     * char, this method will always return an empty list).
     */
    public List<String> getParametersAfterCall(int currentOffset) {
        try {
            currentOffset -= 1;
            char c;
            do {
                currentOffset += 1;
                c = doc.getChar(currentOffset);
            } while (Character.isWhitespace(c));

            if (c == '(') {
                Tuple<List<String>, Integer> insideParentesisToks = getInsideParentesisToks(true, currentOffset, true);
                return insideParentesisToks.o1;
            }

        } catch (Exception e) {
            //ignore any problem getting parameters here
        }

        return new ArrayList<String>();
    }

    private static final Pattern ClassNamePattern = Pattern.compile("\\s*class\\s+(\\w+)");

    public static String getClassNameInLine(String line) {
        Matcher matcher = ClassNamePattern.matcher(line);
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static final class TddPossibleMatches {
        public final String full;
        public final String initialPart;
        public final String secondPart;

        public TddPossibleMatches(String full, String initialPart, String secondPart) {
            this.full = full;
            this.initialPart = initialPart;
            this.secondPart = secondPart;
        }

        @Override
        public String toString() {
            return this.full;
        }

    }

    //0 = full
    //1 = (\\.?)
    //2 = (\\w+)
    //3 = ((\\.\\w+)*)
    //4 = \\s*
    //5 = ((\\()?)
    //
    //i.e.:for a.b.MyCall(
    //0 = a.b.MyCall(
    //1 = null
    //2 = a
    //3 = .b.MyCall
    //4 = null
    //5 = (
    private static final Pattern FunctionCallPattern = Pattern.compile("(\\.?)(\\w+)((\\.\\w+)*)\\s*((\\()?)");

    public List<TddPossibleMatches> getTddPossibleMatchesAtLine() {
        return getTddPossibleMatchesAtLine(this.getAbsoluteCursorOffset());
    }

    private static final int TDD_PART_FULL = 0;
    private static final int TDD_PART_DOT_INITIAL = 1;
    private static final int TDD_PART_PART1 = 2;
    private static final int TDD_PART_PART2 = 3;
    private static final int TDD_PART_PARENS = 5;

    /**
     * @return a list 
     */
    public List<TddPossibleMatches> getTddPossibleMatchesAtLine(int offset) {
        String line = getLine(getLineOfOffset(offset));
        return getTddPossibleMatchesAtLine(line);

    }

    public List<TddPossibleMatches> getTddPossibleMatchesAtLine(String line) {
        List<TddPossibleMatches> ret = new ArrayList<TddPossibleMatches>();
        if (matchesClassLine(line) || matchesFunctionLine(line)) {
            return ret;//In a class or method definition, it should never match.
        }
        Matcher matcher = FunctionCallPattern.matcher(line);
        while (matcher.find()) {
            String dotInitial = matcher.group(TDD_PART_DOT_INITIAL);
            if (dotInitial != null && dotInitial.length() > 0) {
                continue; //skip things as foo().bar() <-- the .bar() should be skipped
            }
            String secondPart = matcher.group(TDD_PART_PART2);
            String parens = matcher.group(TDD_PART_PARENS);
            boolean hasCall = parens != null && parens.length() > 0;
            if (secondPart.length() == 0 && !hasCall) {
                continue; //local var or number
            }
            ret.add(new TddPossibleMatches(matcher.group(TDD_PART_FULL), matcher.group(TDD_PART_PART1), secondPart));
        }
        return ret;
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

}