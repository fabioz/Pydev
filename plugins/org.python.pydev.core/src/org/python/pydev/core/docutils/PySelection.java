/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.shared_core.string.DocIterator;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * Redone the whole class, so that the interface is better defined and no
 * duplication of information is given.
 *
 * Now, it is just used as 'shortcuts' to document and selection settings.
 *
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public final class PySelection extends TextSelectionUtils {

    public static final String[] DEDENT_TOKENS = new String[] { "return", "break", "continue", "pass", "raise",
            //        "yield" -- https://sourceforge.net/tracker/index.php?func=detail&aid=1807411&group_id=85796&atid=577329 (doesn't really end scope)
            //      after seeing the std lib, several cases use yield at the middle of the scope
    };

    public static final String[] CLASS_AND_FUNC_TOKENS = new String[] { "def", "class", };

    public static final String[] FUNC_TOKEN = new String[] { "def" };

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
        super(doc, selection);
    }

    public static PySelection fromTextSelection(TextSelectionUtils ps) {
        return new PySelection(ps.getDoc(), ps.getTextSelection());
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
        super(doc, new TextSelection(doc, getAbsoluteCursorOffset(doc, line, col), len));
    }

    /**
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public PySelection(IDocument doc, int offset) {
        super(doc, new TextSelection(doc, offset, 0));
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
        super(base.doc, new TextSelection(base.doc, base.getAbsoluteCursorOffset(), base.getSelLength()));
    }

    /**
     * @return true if the passed line has a from __future__ import.
     */
    public static boolean isFutureImportLine(String line) {
        List<String> split = StringUtils.split(line, new char[] { ' ', '\t' });
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
            String line = iterator.next();
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
                        for (char c : PyStringUtils.CLOSING_BRACKETS) {
                            int i = line.lastIndexOf(c);
                            if (found == null || found.o2 < i) {
                                found = new Tuple<Character, Integer>(c, i);
                            }
                        }
                        if (found != null) {
                            PythonPairMatcher matcher = new PythonPairMatcher();
                            int openingPeerOffset = matcher.searchForOpeningPeer(this.getLineOffset(lastReturnedLine)
                                    + found.o2, StringUtils.getPeer(found.o1),
                                    found.o1, this.getDoc());
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
            String line = iterator.next();
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

    public static class ActivationTokenAndQual {
        public ActivationTokenAndQual(String activationToken, String qualifier, boolean changedForCalltip,
                boolean alreadyHasParams, boolean isInMethodKeywordParam, int offsetForKeywordParam,
                int calltipOffset) {
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

        public static String[] splitActAndQualifier(String activationToken) {
            //we complete on '.' and '('.
            //' ' gets globals
            //and any other char gets globals on token and templates.

            //we have to get the qualifier. e.g. bla.foo = foo is the qualifier.
            String qualifier = "";
            if (activationToken.indexOf('.') != -1) {
                while (endsWithSomeChar(new char[] { '.', '[' }, activationToken) == false
                        && activationToken.length() > 0) {

                    qualifier = activationToken.charAt(activationToken.length() - 1) + qualifier;
                    activationToken = activationToken.substring(0, activationToken.length() - 1);
                }
            } else { //everything is a part of the qualifier.
                qualifier = activationToken.trim();
                activationToken = "";
            }
            return new String[] { activationToken, qualifier };
        }
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

    public static String getTextForCompletionInConsole(IDocument document, int documentOffset) {
        String lineContentsToCursor;
        try {
            lineContentsToCursor = PySelection.getLineContentsToCursor(document, documentOffset);
        } catch (BadLocationException e1) {
            return "";
        }
        try {
            FastStringBuffer buf = new FastStringBuffer(lineContentsToCursor.length());

            lineContentsToCursor = StringUtils.reverse(lineContentsToCursor);
            ParsingUtils parsingUtils = ParsingUtils.create(lineContentsToCursor);
            int i = 0;
            while (i < parsingUtils.len()) {
                char c = parsingUtils.charAt(i);
                if (c == ']' || c == '}' || c == ')' || c == '\'' || c == '"') { // Check for closing because we're actually going backwards...
                    int initial = i;
                    i = parsingUtils.eatPar(i, null, c);
                    buf.append(lineContentsToCursor.substring(initial, i));
                    if (i < parsingUtils.len()) {
                        buf.append(parsingUtils.charAt(i));
                        i += 1;
                    }
                    continue;
                }
                if (Character.isJavaIdentifierPart(c) || c == '.') {
                    buf.append(c);
                    i += 1;
                    continue;
                }
                break;
            }

            return buf.reverse().toString();
        } catch (Exception e) {
            Log.log(e);
            return lineContentsToCursor;
        }

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

        String[] splitActAndQualifier = ActivationTokenAndQual.splitActAndQualifier(activationToken);
        activationToken = splitActAndQualifier[0];
        String qualifier = splitActAndQualifier[1];
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

    public static boolean hasFromFutureImportUnicode(IDocument document) {
        try {
            FastStringBuffer buf = new FastStringBuffer(100 * 5); //Close to 5 lines

            ParsingUtils parsingUtils = ParsingUtils.create(document);
            int len = parsingUtils.len();

            for (int i = 0; i < len; i++) {
                char c = parsingUtils.charAt(i);
                if (c == '#') {
                    i = parsingUtils.eatComments(null, i);

                } else if (c == '\'' || c == '\"') {
                    try {
                        i = parsingUtils.eatLiterals(null, i);
                    } catch (SyntaxErrorException e) {
                        //ignore
                    }

                } else if (Character.isWhitespace(c)) {
                    //skip

                } else if (c == 'f') { //Possibly some from __future__ import ...
                    i = parsingUtils.eatFromImportStatement(buf, i);
                    if (!PySelection.isFutureImportLine(buf.toString())) {
                        return false;
                    }
                    if (buf.indexOf("unicode_literals") != -1) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
            return false;
        } catch (SyntaxErrorException e) {
            Log.log(e);
            return false;
        }
    }

    /**
     * @return a tuple(start line, end line).
     */
    public Tuple<Integer, Integer> getCurrentMethodStartEndLines() {

        try {
            boolean considerCurrentLine = false;
            LineStartingScope previousLineThatStartsScope = this.getPreviousLineThatStartsScope(FUNC_TOKEN,
                    considerCurrentLine,
                    this.getFirstCharPositionInCurrentCursorOffset());
            if (previousLineThatStartsScope == null) {
                return getFullDocStartEndLines();
            }
            int startLine = previousLineThatStartsScope.iLineStartingScope;
            int minColumn = PySelection.getFirstCharPosition(previousLineThatStartsScope.lineStartingScope);

            int initialOffset = this.getLineOffset(startLine);
            TabNannyDocIterator iterator = new TabNannyDocIterator(getDoc(), true, false,
                    initialOffset);
            if (iterator.hasNext()) {
                iterator.next(); // ignore first one (this is from the current line).
            }
            int lastOffset = initialOffset;
            while (iterator.hasNext()) {
                Tuple3<String, Integer, Boolean> next = iterator.next();
                if (next.o3) {
                    if (next.o1.length() <= minColumn) {
                        break;
                    }
                    lastOffset = next.o2;
                }
            }
            return new Tuple<Integer, Integer>(startLine, this.getLineOfOffset(lastOffset));

            // Can't use the approach below because we may be in an inner scope (thus, there'll be no other opening scope finishing
            // the current one).
            // LineStartingScope nextLineThatStartsScope = this.getNextLineThatStartsScope(FUNC_TOKEN, startLine + 1,
            //         minColumn + 1);
            //
            // if (nextLineThatStartsScope == null) {
            //     int numberOfLines = doc.getNumberOfLines();
            //     if (numberOfLines > 0) {
            //         numberOfLines -= 1;
            //     }
            //     return new Tuple<Integer, Integer>(startLine, numberOfLines);
            // }
            // return new Tuple<Integer, Integer>(startLine, nextLineThatStartsScope.iLineStartingScope - 1);
        } catch (BadLocationException e) {
            return getFullDocStartEndLines();
        } catch (Exception e) {
            Log.log(e);
            return getFullDocStartEndLines();
        }

    }

    private Tuple<Integer, Integer> getFullDocStartEndLines() {
        int numberOfLines = doc.getNumberOfLines();
        if (numberOfLines > 0) {
            numberOfLines -= 1;
        }
        return new Tuple<Integer, Integer>(0, numberOfLines);
    }

}