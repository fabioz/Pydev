/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 10, 2003
 * Author: atotic
 */

package org.python.pydev.core.autoedit;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.CoreTextSelection;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.NoPeerAvailableException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.DocCmd;
import org.python.pydev.shared_core.utils.IDocumentCommand;

/**
 * Class which implements the following behaviors:
 * - indenting: after 'class' or 'def'
 * - replacement: when typing colons or parentheses
 *
 * This class uses the org.python.pydev.core.docutils.DocUtils class extensively
 * for some document-related operations.
 */
public final class PyAutoIndentStrategy implements IHandleScriptAutoEditStrategy {

    private IIndentPrefs prefs;

    private boolean blockSelection;

    private final IAdaptable projectAdaptable;

    public PyAutoIndentStrategy(IAdaptable projectAdaptable) {
        this.projectAdaptable = projectAdaptable;
    }

    public void setIndentPrefs(IIndentPrefs prefs) {
        this.prefs = prefs;
    }

    public IIndentPrefs getIndentPrefs() {
        if (this.prefs == null) {
            if (SharedCorePlugin.inTestMode()) {
                this.prefs = new TestIndentPrefs(true, 4);
            } else {
                this.prefs = new DefaultIndentPrefs(projectAdaptable); //create a new one (because each pyedit may force the tabs differently).
            }
        }
        return this.prefs;
    }

    /**
     * Set indentation automatically after newline.
     *
     * @return tuple with the indentation to be set and a boolean determining if it was found
     * to be within a parenthesis or not.
     */
    private Tuple<String, Boolean> autoIndentNewline(IDocument document, int length, String text, int offset)
            throws BadLocationException {

        if (offset > 0) {
            PySelection selection = new PySelection(document, offset);

            String lineWithoutComments = selection.getLineContentsToCursor(true, true);

            Tuple<Integer, Boolean> tup = determineSmartIndent(offset, document, prefs);
            int smartIndent = tup.o1;
            boolean isInsidePar = tup.o2;

            if (lineWithoutComments.length() > 0) {
                //ok, now let's see the auto-indent
                int curr = lineWithoutComments.length() - 1;
                char lastChar = lineWithoutComments.charAt(curr);

                //we dont want whitespaces
                while (curr > 0 && Character.isWhitespace(lastChar)) {
                    curr--;
                    lastChar = lineWithoutComments.charAt(curr);
                }

                //we have to check if smartIndent is -1 because otherwise we are inside some bracket
                if (smartIndent == -1 && !isInsidePar
                        && StringUtils.isClosingPeer(lastChar)) {
                    //ok, not inside brackets
                    PythonPairMatcher matcher = new PythonPairMatcher(PyStringUtils.BRACKETS);
                    int bracketOffset = selection.getLineOffset() + curr;
                    IRegion region = matcher.match(document, bracketOffset + 1);
                    if (region != null) {
                        if (!PySelection.endsInSameLine(document, region)) {
                            //we might not have a match if there is an error in the program...
                            //e.g. a single ')' without its counterpart.
                            int openingBracketLine = document.getLineOfOffset(region.getOffset());
                            String openingBracketLineStr = PySelection.getLine(document, openingBracketLine);
                            int first = PySelection.getFirstCharPosition(openingBracketLineStr);
                            String initial = getCharsBeforeNewLine(text);
                            text = initial + openingBracketLineStr.substring(0, first);
                            return new Tuple<String, Boolean>(text, isInsidePar);
                        }
                    }
                } else if (smartIndent == -1 && lastChar == ':') {
                    //we have to check if smartIndent is -1 because otherwise we are in a dict
                    //ok, not inside brackets
                    text = indentBasedOnStartingScope(text, selection, false);
                    return new Tuple<String, Boolean>(text, isInsidePar);
                }
            }

            String trimmedLine = lineWithoutComments.trim();

            if (smartIndent >= 0
                    && (PyStringUtils.hasOpeningBracket(trimmedLine) || PyStringUtils.hasClosingBracket(trimmedLine))) {
                return new Tuple<String, Boolean>(makeSmartIndent(text, smartIndent), isInsidePar);
            }
            //let's check for dedents...
            if (PySelection.startsWithDedentToken(trimmedLine)) {
                if (lineWithoutComments.endsWith("\\")) {
                    //Okay, we're in something as return \, where the next line will be part of this statement, so, don't really
                    //go back an indent, but go up an indent.
                    return new Tuple<String, Boolean>(text + prefs.getIndentationString(), isInsidePar);
                }
                return new Tuple<String, Boolean>(dedent(text), isInsidePar);
            }

            boolean indentBasedOnStartingScope = false;
            try {
                if (PySelection.containsOnlyWhitespaces(selection.getLineContentsFromCursor())) {
                    indentBasedOnStartingScope = true;
                }
            } catch (BadLocationException e) {
                //(end of the file)
                indentBasedOnStartingScope = true;
            }

            if (indentBasedOnStartingScope) {
                String lineContentsToCursor = selection.getLineContentsToCursor();
                String trimmed = lineContentsToCursor.trim();
                if (trimmed.length() == 0) {
                    return new Tuple<String, Boolean>(indentBasedOnStartingScope(text, selection, false), isInsidePar);
                } else {
                    boolean endsWithTrippleSingle = trimmed.endsWith("'''");
                    if (endsWithTrippleSingle || trimmed.endsWith("\"\"\"")) {
                        //ok, as we're out of a string scope at this point, this means we just closed a string, so,
                        //we should go back to indent based on starting scope.

                        if (endsWithTrippleSingle) {
                            int cursorLine = -1;
                            try {
                                ParsingUtils parsingUtils = ParsingUtils.create(selection.getDoc(), true);
                                int cursorOffset = selection.getAbsoluteCursorOffset();
                                char c;
                                do {
                                    cursorOffset--;
                                    c = parsingUtils.charAt(cursorOffset);

                                } while (Character.isWhitespace(c));

                                int startOffset = parsingUtils.eatLiteralsBackwards(null, cursorOffset);
                                cursorLine = selection.getLineOfOffset(startOffset);
                            } catch (Exception e) {
                                //may throw error if not balanced or if the char we're at is not a ' or "
                            }

                            if (cursorLine == -1) {
                                cursorLine = selection.getCursorLine();
                            }

                            return new Tuple<String, Boolean>(indentBasedOnStartingScope(text, new PySelection(
                                    selection.getDoc(), cursorLine, 0), false), isInsidePar);

                        }
                    }
                }
            }

        }
        return new Tuple<String, Boolean>(text, false);
    }

    /**
     * @return the text for the indent
     */
    private String indentBasedOnStartingScope(String text, PySelection selection,
            boolean checkForLowestBeforeNewScope) {
        LineStartingScope previousIfLine = selection.getPreviousLineThatStartsScope();
        if (previousIfLine != null) {
            String initial = getCharsBeforeNewLine(text);

            if (previousIfLine.lineWithDedentWhileLookingScope == null) { //no dedent was found
                String indent = PySelection.getIndentationFromLine(previousIfLine.lineStartingScope);

                if (checkForLowestBeforeNewScope && previousIfLine.lineWithLowestIndent != null) {

                    indent = PySelection.getIndentationFromLine(previousIfLine.lineWithLowestIndent);
                    text = initial + indent;

                } else {

                    text = initial + indent + prefs.getIndentationString();

                }

            } else { //some dedent was found
                String indent = PySelection.getIndentationFromLine(previousIfLine.lineWithDedentWhileLookingScope);
                String indentationString = prefs.getIndentationString();

                final int i = indent.length() - indentationString.length();
                if (i > 0 && indent.length() > i) {
                    text = (initial + indent).substring(0, i + 1);
                } else {
                    text = initial; // this can happen if we found a dedent that is 1 level deep
                }
            }

        }
        return text;
    }

    /**
     * Returns the first offset greater than <code>offset</code> and smaller than
     * <code>end</code> whose character is not a space or tab character. If no such
     * offset is found, <code>end</code> is returned.
     *
     * @param document the document to search in
     * @param offset the offset at which searching start
     * @param end the offset at which searching stops
     * @return the offset in the specified range whose character is not a space or tab
     * @exception BadLocationException if position is an invalid range in the given document
     */
    private int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
        while (offset < end) {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
            offset++;
        }
        return end;
    }

    private void autoIndentSameAsPrevious(IDocument d, IDocumentCommand c) {
        String txt = autoIndentSameAsPrevious(d, c.getOffset(), c.getText(), true);
        if (txt != null) {
            c.setText(txt);
        }
    }

    /**
     * Copies the indentation of the previous line.
     *
     * @param d the document to work on
     * @param text the string that should added to the start of the returned string
     * @param considerEmptyLines whether we should consider empty lines in this function
     * @param c the command to deal with
     *
     * @return a string with text+ the indentation found in the previous line (or previous non-empty line).
     */
    private String autoIndentSameAsPrevious(IDocument d, int offset, String text, boolean considerEmptyLines) {

        if (offset == -1 || d.getLength() == 0) {
            return null;
        }

        try {
            // find start of line
            IRegion info = d.getLineInformationOfOffset(offset);
            String line = d.get(info.getOffset(), info.getLength());

            if (!considerEmptyLines) {
                int currLine = d.getLineOfOffset(offset);
                while (PySelection.containsOnlyWhitespaces(line)) {
                    currLine--;
                    if (currLine < 0) {
                        break;
                    }
                    info = d.getLineInformation(currLine);
                    line = d.get(info.getOffset(), info.getLength());
                }
            }

            int start = info.getOffset();

            // find white spaces
            int end = findEndOfWhiteSpace(d, start, offset);

            FastStringBuffer buf = new FastStringBuffer(text, end - start + 1);
            if (end > start) {
                // append to input
                buf.append(d.get(start, end - start));
            }

            return buf.toString();

        } catch (BadLocationException excp) {
            // stop work
            return null;
        }
    }

    private String dedent(String text) {
        String indentationString = prefs.getIndentationString();
        int indentationLength = indentationString.length();
        int len = text.length();

        if (len >= indentationLength) {
            text = text.substring(0, len - indentationLength);
        }
        return text;
    }

    private static Tuple<String, Integer> removeFirstIndent(String text, IIndentPrefs prefs) {
        String indentationString = prefs.getIndentationString();
        if (text.startsWith(indentationString)) {
            return new Tuple<String, Integer>(text.substring(indentationString.length()), indentationString.length());
        }
        return new Tuple<String, Integer>(text, 0);
    }

    /**
     * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(IDocument, IDocumentCommand)
     */
    public void customizeDocumentCommand(IDocument document, IDocumentCommand command) {
        if (blockSelection || !command.getDoIt()) {
            //in block selection, leave all as is and just change tabs/spaces.
            getIndentPrefs().convertToStd(document, command);
            return;
        }
        char c;
        if (command.getText().length() == 1) {
            c = command.getText().charAt(0);
        } else {
            c = '\0';
        }

        String contentType = ParsingUtils.getContentType(document, command.getOffset());

        switch (c) {
            case '"':
            case '\'':
                handleLiteral(document, command, contentType.equals(ParsingUtils.PY_DEFAULT), c);
                return;
        }

        final boolean tabStopInComments = getIndentPrefs().getTabStopInComment();

        // super idents newlines the same amount as the previous line
        final boolean isNewLine = isNewLineText(document, command.getLength(), command.getText());

        if (!contentType.equals(ParsingUtils.PY_DEFAULT)) {
            //the indentation is only valid for things in the code (comments should not be indented).
            //(that is, if it is not a new line... in this case, it may have to be indented)
            if (isNewLine) {
                if (ParsingUtils.isStringContentType(contentType)) {
                    //within string, just regular indent...
                    autoIndentSameAsPrevious(document, command);
                    return;
                }
            } else {
                //not newline
                if (ParsingUtils.isCommentContentType(contentType) && c == '\t' && tabStopInComments) {
                    //within a comment...
                    /* do nothing, but don't return */
                } else {
                    //we have to take care about tabs anyway
                    getIndentPrefs().convertToStd(document, command);
                    return;
                }
            }
        }

        try {
            if (isNewLine) {
                customizeNewLine(document, command);
                getIndentPrefs().convertToStd(document, command);
                return;
            }

            if (c == '\0') {
                //In some paste with more contents (c was not set), just convert tabs/spaces and go on...
                getIndentPrefs().convertToStd(document, command);
                return;
            }

            if (c == '\t') {
                handleTab(document, command);
                getIndentPrefs().convertToStd(document, command);
                return;
            }

            getIndentPrefs().convertToStd(document, command);

            switch (c) {
                case '[':
                case '{':
                    if (prefs.getAutoParentesis()) {
                        PySelection ps = new PySelection(document, command.getOffset());
                        char peer = StringUtils.getPeer(c);
                        if (shouldClose(ps, c, peer)) {
                            command.setShiftsCaret(false);
                            command.setText(c + "" + peer);
                            command.setCaretOffset(command.getOffset() + 1);
                        }
                    }
                    return;

                case '(':
                    handleParens(document, command, prefs, considerOnlyCurrentLine);
                    return;

                case ':':
                    /*
                     * The following code will auto-replace colons in function
                     * declaractions
                     * e.g.,
                     * def something(self):
                     *                    ^ cursor before the end colon
                     *
                     * Typing another colon (i.e, ':') at that position will not insert
                     * another colon
                     */
                    if (prefs.getAutoColon()) {
                        performColonReplacement(document, command);
                    }

                    /*
                     * Now, let's also check if we are in an 'else:' or 'except:' or 'finally:' that must be dedented in the doc
                     */
                    autoDedentAfterColon(document, command, prefs);
                    return;

                case ' ':
                    /*
                     * this is a space... so, if we are in 'from xxx ', we may auto-write
                     * the import
                     */
                    if (prefs.getAutoWriteImport()) {
                        PySelection ps = new PySelection(document, command.getOffset());
                        String completeLine = ps.getLineWithoutCommentsOrLiterals();
                        String lineToCursor = ps.getLineContentsToCursor().trim();
                        String lineContentsFromCursor = ps.getLineContentsFromCursor();

                        if (completeLine.indexOf(" import ") == -1
                                && StringUtils.leftTrim(completeLine).startsWith("from ")
                                && !completeLine.startsWith("import ") && !completeLine.endsWith(" import")
                                && !lineToCursor.endsWith(" import") && !lineContentsFromCursor.startsWith("import")
                                && !completeLine.startsWith("cimport ") && !completeLine.endsWith(" cimport")
                                && !lineToCursor.endsWith(" cimport")
                                && !lineContentsFromCursor.startsWith("cimport")) {

                            String importsTipperStr = ImportsSelection.getImportsTipperStr(lineToCursor,
                                    false).importsTipperStr;
                            if (importsTipperStr.length() > 0) {
                                if (!isCython) {
                                    // On cython it could be a cimport, so, skip it.
                                    command.setText(" import ");
                                }
                            }
                        }
                    }

                    /*
                     * Now, let's also check if we are in an 'elif ' that must be dedented in the doc
                     */
                    autoDedentElif(document, command, getIndentPrefs());
                    return;

                case ')':
                case ']':
                case '}':
                    /*
                     * If the command is some kind of parentheses or brace, and there's
                     * already a matching one, don't insert it. Just move the cursor to
                     * the next space.
                     */
                    if (prefs.getAutoBraces()) {
                        // you can only do the replacement if the next character already there is what the user is trying to input

                        if (command.getOffset() < document.getLength()
                                && document.get(command.getOffset(), 1).equals(command.getText())) {
                            // the following searches through each of the end braces and
                            // sees if the command has one of them

                            boolean found = false;
                            for (int i = 1; i <= PyStringUtils.BRACKETS.length && !found; i += 2) {
                                char b = PyStringUtils.BRACKETS[i];
                                if (b == c) {
                                    found = true;
                                    performPairReplacement(document, command);
                                }
                            }
                        }
                    }
                    return;

            }

        }
        /*
         * If something goes wrong, you want to know about it, especially in a
         * unit test. If you don't rethrow the exception, unit tests will pass
         * even though you threw an exception.
         */
        catch (BadLocationException e) {
            // screw up command.text so unit tests can pick it up
            command.setText("BadLocationException");
            throw new RuntimeException(e);
        }
    }

    public static boolean isNewLineText(IDocument document, int length, String text) {
        return length == 0 && text != null && TextSelectionUtils.endsWithNewline(document, text) && text.length() < 3; //could be \r\n
    }

    /**
     * Called right after a '('
     */
    public static void handleParens(IDocument document, IDocumentCommand command, IIndentPrefs prefs,
            boolean considerOnlyCurrentLine)
            throws BadLocationException {
        /*
         * Now, let's also check if we are in an 'elif ' that must be dedented in the doc
         */
        autoDedentElif(document, command, prefs);

        customizeParenthesis(document, command, considerOnlyCurrentLine, prefs);
    }

    private boolean considerOnlyCurrentLine = false;

    @Override
    public void setConsiderOnlyCurrentLine(boolean considerOnlyCurrentLine) {
        this.considerOnlyCurrentLine = considerOnlyCurrentLine;
    }

    public boolean getConsiderOnlyCurrentLine() {
        return this.considerOnlyCurrentLine;
    }

    /**
     * Called right after a ' or "
     */
    private void handleLiteral(IDocument document, IDocumentCommand command, boolean isDefaultContext,
            char literalChar) {
        if (!prefs.getAutoLiterals()) {
            return;
        }
        TextSelectionUtils ps = new TextSelectionUtils(document, new CoreTextSelection(document, command.getOffset(),
                command.getLength()));
        if (command.getLength() > 0) {
            try {
                //We have more contents selected. Delete it so that we can properly use the heuristics.
                ps.deleteSelection();
                command.setLength(0);
                ps.setSelection(command.getOffset(), command.getOffset());
            } catch (BadLocationException e) {
            }
        }

        try {
            char nextChar = ps.getCharAfterCurrentOffset();
            if (Character.isJavaIdentifierPart(nextChar)) {
                //we're just before a word (don't try to do anything in this case)
                //e.g. |var (| is cursor position)
                return;
            }
        } catch (BadLocationException e) {
        }

        String cursorLineContents = ps.getCursorLineContents();
        if (cursorLineContents.indexOf(literalChar) == -1) {
            if (!isDefaultContext) {
                //only add additional chars if on default context.
                return;
            }
            command.setText(StringUtils.getWithClosedPeer(literalChar));
            command.setShiftsCaret(false);
            command.setCaretOffset(command.getOffset() + 1);
            return;
        }

        boolean balanced = isLiteralBalanced(cursorLineContents);

        Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(literalChar);

        int matchesBefore = beforeAndAfterMatchingChars.o1.length();
        int matchesAfter = beforeAndAfterMatchingChars.o2.length();

        boolean hasMatchesBefore = matchesBefore != 0;
        boolean hasMatchesAfter = matchesAfter != 0;

        if (!hasMatchesBefore && !hasMatchesAfter) {
            //if it's not balanced, this char would be the closing char.
            if (balanced) {
                if (!isDefaultContext) {
                    //only add additional chars if on default context.
                    return;
                }
                command.setText(StringUtils.getWithClosedPeer(literalChar));
                command.setShiftsCaret(false);
                command.setCaretOffset(command.getOffset() + 1);
            }
        } else {
            //we're right after or before a " or '

            if (matchesAfter == 1) {
                //just walk the caret
                command.setText("");
                command.setShiftsCaret(false);
                command.setCaretOffset(command.getOffset() + 1);
            }
        }
    }

    /**
     * @return true if the passed string has balanced ' and "
     */
    private boolean isLiteralBalanced(String cursorLineContents) {
        ParsingUtils parsingUtils = ParsingUtils.create(cursorLineContents, true);

        int offset = 0;
        int end = cursorLineContents.length();
        boolean balanced = true;
        while (offset < end) {
            char curr = cursorLineContents.charAt(offset++);
            if (curr == '"' || curr == '\'') {
                int eaten;
                try {
                    eaten = parsingUtils.eatLiterals(null, offset - 1) + 1;
                } catch (SyntaxErrorException e) {
                    balanced = false;
                    break;
                }
                if (eaten > offset) {
                    offset = eaten;
                }
            }
        }
        return balanced;
    }

    private void handleTab(IDocument document, IDocumentCommand command) throws BadLocationException {
        PySelection ps = new PySelection(document, command.getOffset());
        //it is a tab
        String lineContentsToCursor = ps.getLineContentsToCursor();
        int currSize = lineContentsToCursor.length();
        int cursorLine = ps.getCursorLine();

        //current line is empty
        if (lineContentsToCursor.trim().length() == 0) {
            String nextLine = ps.getLine(cursorLine + 1);

            String prevLine = ps.getLine(cursorLine - 1);
            boolean forceTryOnNext = false;
            if (prevLine.trim().length() == 0) {
                //previous line is empty, so, if the next line has contents, use it to make the match.
                if (nextLine.trim().length() > 0) {
                    forceTryOnNext = true;
                }
            }

            if (forceTryOnNext || nextLine.trim().startsWith("@") || PySelection.matchesFunctionLine(nextLine)) {
                int firstCharPosition = PySelection.getFirstCharPosition(nextLine);
                if (currSize < firstCharPosition) {
                    String txt = nextLine.substring(currSize, firstCharPosition);
                    //as it's the same indentation from the next line, we don't have to applyDefaultForTab.
                    command.setText(txt);
                    return;
                }
            }
        }

        if (cursorLine > 0) {
            //this is to know which would be expected if it was a new line in the previous line
            //(so that we know the 'expected' output
            IRegion prevLineInfo = document.getLineInformation(cursorLine - 1);
            int prevLineEndOffset = prevLineInfo.getOffset() + prevLineInfo.getLength();
            String prevExpectedIndent = autoIndentSameAsPrevious(document, prevLineEndOffset, "\n", false);
            String txt = prevExpectedIndent;
            Tuple<String, Boolean> prevLineTup = autoIndentNewline(document, 0, txt, prevLineEndOffset);
            txt = prevLineTup.o1;
            txt = txt.substring(1);//remove the newline
            prevExpectedIndent = prevExpectedIndent.substring(1);

            if (txt.length() > 0) {
                //now, we should not apply that indent if we are already at the 'max' indent in this line
                //(or better: we should go to that max if it would pass it)
                int sizeExpected = txt.length();
                int sizeApplied = currSize + sizeExpected;

                if (currSize >= sizeExpected) {
                    //ok, we already passed what we expected from the indentation, so, let's indent
                    //to the next 'expected' position...

                    boolean applied = false;
                    //handle within parenthesis
                    if (prevLineTup.o2) {
                        int len = sizeApplied - sizeExpected;
                        if (prevExpectedIndent.length() > len) {
                            command.setText(prevExpectedIndent.substring(len));
                            applied = true;
                        }
                    }

                    if (!applied) {
                        applyDefaultForTab(command, currSize);
                    }

                } else if (sizeExpected == sizeApplied) {
                    if (command.getLength() == 0) {
                        ps.deleteSpacesAfter(command.getOffset());
                    }
                    command.setText(txt);
                } else if (sizeApplied > sizeExpected) {
                    ps.deleteSpacesAfter(command.getOffset());
                    command.setText(txt.substring(0, sizeExpected - currSize));
                }
            } else {
                applyDefaultForTab(command, currSize);
            }

        } else { //cursorLine == 0
            applyDefaultForTab(command, currSize);
        }
    }

    public static void customizeParenthesis(IDocument document, IDocumentCommand command,
            boolean considerOnlyCurrentLine, IIndentPrefs prefs) throws BadLocationException {
        if (prefs.getAutoParentesis()) {
            PySelection ps = new PySelection(document, command.getOffset());

            if (shouldClose(ps, '(', ')')) {
                customizeParenthesis2(command, considerOnlyCurrentLine, prefs, ps);
            }
        }
    }

    public static void customizeParenthesis2(IDocumentCommand command, boolean considerOnlyCurrentLine,
            IIndentPrefs prefs,
            PySelection ps) {
        final String line = ps.getLine();
        boolean hasClass = line.indexOf("class ") != -1;
        boolean hasClassMethodDef = line.indexOf(" def ") != -1 || line.indexOf("\tdef ") != -1;
        boolean hasMethodDef = line.indexOf("def ") != -1;
        boolean hasDoublePoint = line.indexOf(":") != -1;

        command.setShiftsCaret(false);
        if (!hasDoublePoint && (hasClass || hasClassMethodDef || hasMethodDef)) {
            if (hasClass) {
                //command.text = "(object):"; //TODO: put some option in the interface for that
                //command.caretOffset = command.getOffset() + 7;
                command.setText("():");
                command.setCaretOffset(command.getOffset() + 1);

            } else if (hasClassMethodDef && prefs.getAutoAddSelf()) {
                String prevLine = ps.getLine(ps.getCursorLine() - 1);
                if (prevLine.indexOf("@classmethod") != -1) {
                    command.setText("(cls):");
                    command.setCaretOffset(command.getOffset() + 4);

                } else if (prevLine.indexOf("@staticmethod") != -1) {
                    command.setText("():");
                    command.setCaretOffset(command.getOffset() + 1);

                } else {

                    boolean addRegularWithSelf = true;
                    if (!considerOnlyCurrentLine) {
                        //ok, also analyze the scope we're in (otherwise, if we only have the current line
                        //that's the best guess we can give).
                        int firstCharPosition = PySelection.getFirstCharPosition(line);

                        LineStartingScope scopeStart = ps.getPreviousLineThatStartsScope(
                                PySelection.CLASS_AND_FUNC_TOKENS, false, firstCharPosition);

                        if (scopeStart != null) {
                            if (scopeStart.lineStartingScope != null
                                    && scopeStart.lineStartingScope.indexOf("def ") != -1) {
                                int iCurrDef = PySelection.getFirstCharPosition(line);
                                int iPrevDef = PySelection.getFirstCharPosition(scopeStart.lineStartingScope);
                                if (iCurrDef > iPrevDef) {
                                    addRegularWithSelf = false;

                                } else if (iCurrDef == iPrevDef) {
                                    if (scopeStart.lineStartingScope.indexOf("self") == -1) {
                                        //only add self if the one in the same level also has it.
                                        //with a 'gotcha': if it's a classmethod or staticmethod, we
                                        //should still add it.
                                        if (scopeStart.iLineStartingScope <= 0) {
                                            addRegularWithSelf = false;
                                        } else {
                                            addRegularWithSelf = false;
                                            int i = scopeStart.iLineStartingScope - 1;
                                            String line2;
                                            do {
                                                line2 = ps.getLine(i).trim();
                                                i--;
                                                if (line2.startsWith("@classmethod")
                                                        || line2.startsWith("@staticmethod")) {
                                                    addRegularWithSelf = true;
                                                    break;
                                                }
                                            } while (line2.startsWith("@")); //check all the available decorators...

                                        }
                                    }
                                }
                            }
                        } else {
                            addRegularWithSelf = false;
                        }
                    } else {
                        // i.e.: we want to consider only the current line, so, we can't base ourselves on the ident scope.
                        // so, just base ourselves on the def position.
                        addRegularWithSelf = false;
                        int i = Math.max(line.indexOf("async def "), line.indexOf("async def\t"));
                        if (i >= 0) {
                            if (i == 0) {
                                addRegularWithSelf = false;
                            } else {
                                addRegularWithSelf = true;
                            }
                        } else {
                            i = Math.max(line.indexOf("def "), line.indexOf("def\t"));
                            if (i >= 0) {
                                if (i == 0) {
                                    addRegularWithSelf = false;
                                } else {
                                    addRegularWithSelf = true;
                                }
                            }
                        }

                    }
                    if (addRegularWithSelf) {
                        if ("__exit__".equals(ps.getFunctionName(line))) {
                            command.setText("(self, exc_type, exc_val, exc_tb):");
                        } else {
                            command.setText("(self):");
                        }
                        command.setCaretOffset(command.getOffset() + 5);
                    } else {
                        command.setText("():");
                        command.setCaretOffset(command.getOffset() + 1);
                    }

                }
            } else if (hasMethodDef) {
                command.setText("():");
                command.setCaretOffset(command.getOffset() + 1);
            } else {
                throw new RuntimeException(PyAutoIndentStrategy.class.toString()
                        + ": customizeDocumentCommand()");
            }
        } else {
            command.setText("()");
            command.setCaretOffset(command.getOffset() + 1);
        }
    }

    @Override
    public void customizeNewLine(IDocument document, IDocumentCommand command) throws BadLocationException {
        prefs = getIndentPrefs();
        autoIndentSameAsPrevious(document, command);
        if (prefs.getSmartIndentPar()) {
            PySelection selection = new PySelection(document, command.getOffset());
            if (selection.getCursorLineContents().trim().length() > 0) {
                command.setText(
                        autoIndentNewline(document, command.getLength(), command.getText(), command.getOffset()).o1);
                if (PySelection.containsOnlyWhitespaces(selection.getLineContentsToCursor())) {
                    command.setCaretOffset(command.getOffset() + selection.countSpacesAfter(command.getOffset()));
                }
            }
        } else {
            TextSelectionUtils selection = new TextSelectionUtils(document, command.getOffset());
            if (selection.getLineContentsToCursor().trim().endsWith(":")) {
                command.setText(command.getText() + prefs.getIndentationString());
            }
        }
    }

    /**
     * Updates the text to the next tab position
     * @param command the command to be edited
     * @param lineContentsToCursorLen the current cursor position at the current line
     */
    private void applyDefaultForTab(IDocumentCommand command, int lineContentsToCursorLen) {
        IIndentPrefs prefs = getIndentPrefs();
        if (prefs.getUseSpaces(true)) {
            int tabWidth = getIndentPrefs().getTabWidth();

            int mod = (lineContentsToCursorLen + tabWidth) % tabWidth;
            command.setText(StringUtils.createSpaceString(tabWidth - mod));
        } else {
            //do nothing (a tab is already a tab)
        }

    }

    public static Tuple<String, Integer> autoDedentStatement(IDocument document, IDocumentCommand command, String tok,
            String[] tokens, IIndentPrefs prefs) throws BadLocationException {
        return autoDedentStatement(document, command, tok, tokens, prefs, true);
    }

    /**
     * This function makes the else auto-dedent (if available)
     * @return the new indent and the number of chars it has been dedented (so, that has to be considered as a shift to the left
     * on subsequent things).
     */
    public static Tuple<String, Integer> autoDedentStatement(IDocument document, IDocumentCommand command, String tok,
            String[] tokens, IIndentPrefs prefs, boolean applyDedentInDocument) throws BadLocationException {
        if (prefs.getAutoDedentElse() && command.getDoIt()) {
            PySelection ps = new PySelection(document, command.getOffset());
            String lineContents = ps.getCursorLineContents();
            if (lineContents.trim().equals(tok)) {
                String previousIfLine = ps.getPreviousLineThatStartsWithToken(tokens);
                if (previousIfLine != null) {
                    String ifIndent = PySelection.getIndentationFromLine(previousIfLine);
                    String lineIndent = PySelection.getIndentationFromLine(lineContents);

                    String indent = prefs.getIndentationString();
                    if (lineIndent.length() == ifIndent.length() + indent.length()) {
                        Tuple<String, Integer> dedented = removeFirstIndent(lineContents, prefs);
                        if (applyDedentInDocument) {
                            ps.replaceLineContentsToSelection(dedented.o1);
                        }
                        command.setOffset(command.getOffset() - dedented.o2);
                        return dedented;
                    }
                }
            }
        }
        return null;
    }

    public static Tuple<String, Integer> autoDedentAfterColon(IDocument document, IDocumentCommand command,
            IIndentPrefs prefs) throws BadLocationException {
        Tuple<String, Integer> ret = null;
        if ((ret = autoDedentStatement(document, command, "else", PySelection.TOKENS_BEFORE_ELSE, prefs)) != null) {
            return ret;
        }
        if ((ret = autoDedentStatement(document, command, "except", PySelection.TOKENS_BEFORE_EXCEPT,
                prefs)) != null) {
            return ret;
        }
        if ((ret = autoDedentStatement(document, command, "finally", PySelection.TOKENS_BEFORE_FINALLY,
                prefs)) != null) {
            return ret;
        }
        return null;
    }

    /**
     * This function makes the else auto-dedent (if available)
     * @return the new indent and the number of chars it has been dedented (so, that has to be considered as a shift to the left
     * on subsequent things).
     */
    public static Tuple<String, Integer> autoDedentElif(IDocument document, IDocumentCommand command,
            IIndentPrefs prefs)
            throws BadLocationException {
        return autoDedentStatement(document, command, "elif", PySelection.TOKENS_BEFORE_ELIF, prefs);
    }

    /**
     * Create the indentation string after comma and a newline.
     *
     * @param document
     * @param text
     * @param offset
     * @param selection
     * @return Indentation String
     * @throws BadLocationException
     */
    private String makeSmartIndent(String text, int smartIndent) throws BadLocationException {
        if (smartIndent > 0) {
            String initial = text;

            // Discard everything but the newline from initial, since we'll
            // build the smart indent from scratch anyway.
            initial = getCharsBeforeNewLine(initial);

            // Create the actual indentation string
            String indentationString = prefs.getIndentationString();
            int indentationSteps = smartIndent / prefs.getTabWidth();
            int spaceSteps = smartIndent % prefs.getTabWidth();

            StringBuffer b = new StringBuffer(smartIndent);
            while (indentationSteps > 0) {
                indentationSteps -= 1;
                b.append(indentationString);
            }

            if (prefs.getUseSpaces(true)) {
                while (spaceSteps >= 0) {
                    spaceSteps -= 1;
                    b.append(" ");
                }
            }

            return initial + b.toString();
        }
        return text;
    }

    /**
     * @param initial
     * @return
     */
    private String getCharsBeforeNewLine(String initial) {
        int initialLength = initial.length();
        for (int i = 0; i < initialLength; i++) {
            char theChar = initial.charAt(i);
            // This covers all cases I know of, but if there is any platform
            // with weird newline then this would need to be smarter.
            if (theChar != '\r' && theChar != '\n') {
                if (i > 0) {
                    initial = initial.substring(0, i);
                }
                break;
            }
        }
        return initial;
    }

    /**
     * Private function which is called when a colon is the command.
     *
     * The following code will auto-replace colons in function declaractions
     * e.g., def something(self): ^ cursor before the end colon
     *
     * Typing another colon (i.e, ':') at that position will not insert another
     * colon
     *
     * @param document
     * @param command
     * @throws BadLocationException
     */
    private void performColonReplacement(IDocument document, IDocumentCommand command) {
        PySelection ps = new PySelection(document, command.getOffset());
        int absoluteOffset = ps.getAbsoluteCursorOffset();
        int documentLength = ps.getDoc().getLength();

        // need to check whether whether we're at the very end of the document
        if (absoluteOffset < documentLength) {
            try {
                char currentCharacter = document.getChar(absoluteOffset);

                if (currentCharacter == ':') {
                    command.setText("");
                    command.setCaretOffset(command.getOffset() + 1);
                }

            } catch (BadLocationException e) {
                // should never happen because I just checked the length
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * Private function to call to perform any replacement of braces.
     *
     * The Eclipse Java editor does this by default, and it is very useful. If
     * you try to insert some kind of pair, be it a parenthesis or bracket in
     * Java, the character will not insert and instead the editor just puts your
     * cursor at the next position.
     *
     * This function performs the equivalent for the Python editor.
     *
     * @param document
     * @param command if the command does not contain a brace, this function does nothing.
     * @throws BadLocationException
     */
    private void performPairReplacement(IDocument document, IDocumentCommand command) throws BadLocationException {
        boolean skipChar = canSkipCloseParenthesis(document, command);
        if (skipChar) {
            //if we have the same number of peers, we want to eat the char
            command.setText("");
            command.setCaretOffset(command.getOffset() + 1);
        }
    }

    /**
     * @return true if we should skip a ), ] or }
     */
    @Override
    public boolean canSkipCloseParenthesis(IDocument document, IDocumentCommand command) throws BadLocationException {
        PySelection ps = new PySelection(document, command.getOffset());

        int absoluteCursorOffset = ps.getAbsoluteCursorOffset();
        if (absoluteCursorOffset >= document.getLength()) {
            return false;
        }
        char c = ps.getCharAtCurrentOffset();
        if (command.getText() != null && command.getText().length() == 1 && command.getText().charAt(0) == c) {
            try {
                char peer = StringUtils.getPeer(c);

                FastStringBuffer doc = new FastStringBuffer(document.get(), 2);
                //it is not enough just counting the chars, we have to ignore those that are within comments or literals.
                ParsingUtils.removeCommentsWhitespacesAndLiterals(doc, false);
                int chars = StringUtils.countChars(c, doc);
                int peers = StringUtils.countChars(peer, doc);

                boolean skipChar = chars == peers;
                return skipChar;
            } catch (NoPeerAvailableException e) {
                return false;
            } catch (SyntaxErrorException e) {
                throw new RuntimeException(e);//not expected!
            }
        }
        return false;
    }

    /**
     * @return true if we should close the opening pair (parameter c) and false if we shouldn't
     */
    public static boolean shouldClose(PySelection ps, char c, char peer) throws BadLocationException {
        PythonPairMatcher matcher = new PythonPairMatcher(PyStringUtils.BRACKETS);
        String lineContentsFromCursor = ps.getLineContentsFromCursor();

        for (int i = 0; i < lineContentsFromCursor.length(); i++) {
            char charAt = lineContentsFromCursor.charAt(i);
            if (!Character.isWhitespace(charAt)) {

                if (charAt == ',') {
                    break;
                }
                if (StringUtils.isClosingPeer(charAt)) {
                    break;
                }

                return false;
            }
        }

        //Ok, we have to analyze the current context and see if each closing peer
        //in this context has a match. If one doesn't, we won't close it.
        LineStartingScope nextLineThatStartsScope = ps.getNextLineThatStartsScope();
        int lineStartingNextScope;
        if (nextLineThatStartsScope == null) {
            lineStartingNextScope = Integer.MAX_VALUE;
        } else {
            lineStartingNextScope = nextLineThatStartsScope.iLineStartingScope;
        }

        int closingPeerLine;
        int closingPeerFoundAtOffset = ps.getAbsoluteCursorOffset() - 1; //start to search at the current position

        do {
            //closingPeerFoundAtOffset doesn't need +1 here as it's already added in the matcher.
            closingPeerFoundAtOffset = matcher.searchForClosingPeer(closingPeerFoundAtOffset, c, peer, ps.getDoc());
            if (closingPeerFoundAtOffset == -1) {
                //no more closing peers there, ok to go
                return true;
            }

            //the +1 is needed because we match closing ones that are right before the current cursor
            IRegion match = matcher.match(ps.getDoc(), closingPeerFoundAtOffset + 1);
            if (match == null) {
                //we don't have a match for a close, so, this open is that match.
                return false;
            }

            try {
                closingPeerLine = ps.getDoc().getLineOfOffset(closingPeerFoundAtOffset);
            } catch (Exception e) {
                break;
            }
        } while (lineStartingNextScope > closingPeerLine);

        return true;
    }

    /**
     * Return smart indent amount for new line. This should be done for
     * multiline structures like function parameters, tuples, lists and
     * dictionaries.
     *
     * Example:
     *
     * a=foo(1, #
     *
     * We would return the indentation needed to place the caret at the #
     * position.
     *
     * @param document The document
     * @param offset The document offset of the last character on the previous line
     * @param ps
     * @return indent, or -1 if smart indent could not be determined (fall back to default)
     * and a boolean indicating if we're inside a parenthesis
     */
    public Tuple<Integer, Boolean> determineSmartIndent(final int offset, IDocument document, IIndentPrefs prefs)
            throws BadLocationException {

        PythonPairMatcher matcher = new PythonPairMatcher(PyStringUtils.BRACKETS);
        int openingPeerOffset = matcher.searchForAnyOpeningPeer(offset, document);
        if (openingPeerOffset == -1) {
            return new Tuple<Integer, Boolean>(-1, false);
        }

        final IRegion lineInformationOfOffset = document.getLineInformationOfOffset(openingPeerOffset);
        //ok, now, if the opening peer is not on the line we're currently, we do not want to make
        //an 'auto-indent', but keep the current indentation level
        boolean openingPeerIsInCurrentLine = PySelection.isInside(offset, lineInformationOfOffset);

        boolean indentToParAsPep8 = prefs.getIndentToParAsPep8();
        boolean indentToParLevel = prefs.getIndentToParLevel();
        int indentAfterParWidth = prefs.getIndentAfterParWidth();

        final PySelection ps = new PySelection(document, offset);
        final String lineContentsToCursor = ps.getLineContentsToCursor();
        if (indentToParAsPep8) {
            String trimmed = lineContentsToCursor.trim();
            if (trimmed.endsWith("(") || trimmed.endsWith("[") || trimmed.endsWith("{")) {
                indentToParLevel = false;
                // If we're in a class or def line, add an additional indentation level.
                if (PySelection.matchesFunctionLine(trimmed) || PySelection.matchesClassLine(trimmed)) {
                    if (prefs.getUseSpaces(true)) {
                        indentAfterParWidth = 2;

                    } else {
                        // We'll end up removing one tab afterwards
                        indentAfterParWidth = 3;
                    }
                } else {
                    if (prefs.getUseSpaces(true)) {
                        indentAfterParWidth = 1;

                    } else {
                        // We'll end up removing one tab afterwards
                        indentAfterParWidth = 2;
                    }
                }
            } else {
                indentToParLevel = true;
            }
        }

        int len = -1;
        String contents = "";
        if (indentToParLevel) {
            //now, a catch, if we didn't change the indent level, we've to indent in the same level
            //as the previous line, as this means that the user 'customized' the indent level at this place.
            if (!openingPeerIsInCurrentLine) {
                try {
                    final char openingChar = document.getChar(openingPeerOffset);
                    final int closingPeerOffset = matcher.searchForClosingPeer(openingPeerOffset, openingChar,
                            StringUtils.getPeer(openingChar), document);
                    if (closingPeerOffset == -1 || offset <= closingPeerOffset) {
                        if (PyStringUtils.hasUnbalancedClosingPeers(lineContentsToCursor)) {
                            // we have a closing peer which closes in another line, compute things based on that level.
                            for (int peerI = lineContentsToCursor.length() - 1; peerI >= 0; peerI--) {
                                char c = lineContentsToCursor.charAt(peerI);
                                if (StringUtils.isClosingPeer(c)) {
                                    final int foundAtOffset = matcher.searchForOpeningPeer(
                                            ps.getLineOffset() + peerI, StringUtils.getPeer(c), c, document);
                                    if (foundAtOffset != -1) {
                                        final int currLineOffset = ps.getLineOffset();
                                        IRegion lineInformationOfOffset2 = document
                                                .getLineInformationOfOffset(foundAtOffset);
                                        // I.e.: don't use info if it's in the same line
                                        if (currLineOffset != lineInformationOfOffset2.getOffset()) {
                                            // Now, if the parens on that line are all properly closed, simply use
                                            // the indentation of that line, otherwise, compute what would be the
                                            // indentation based on the unbalanced parens of that line.
                                            String line = ps
                                                    .getLine(ps.getLineOfOffset(lineInformationOfOffset2.getOffset()));
                                            for (int openPeerI = 0; openPeerI < line.length(); openPeerI++) {
                                                char f = line.charAt(openPeerI);
                                                if (StringUtils.isOpeningPeer(f)) {
                                                    final int closingPeerOffsetFound = matcher.searchForClosingPeer(
                                                            lineInformationOfOffset2.getOffset() + openPeerI, f,
                                                            StringUtils.getPeer(f), document);
                                                    if (closingPeerOffsetFound == -1
                                                            || closingPeerOffsetFound > offset) {
                                                        return this.determineSmartIndent(
                                                                lineInformationOfOffset2.getLength()
                                                                        + lineInformationOfOffset2.getOffset(),
                                                                document, prefs);
                                                    }
                                                    break;
                                                }
                                            }
                                            return new Tuple<Integer, Boolean>(
                                                    TextSelectionUtils.getFirstCharRelativePosition(document,
                                                            foundAtOffset) - 1,
                                                    true);
                                        }
                                    }
                                }
                            }
                        }
                        return new Tuple<Integer, Boolean>(-1, true); // True because we're inside a parens
                    }

                } catch (Exception e) {
                    Log.log(e);
                    //Something unexpected happened... (document changed?)
                    return new Tuple<Integer, Boolean>(-1, true); // True because we're inside a parens
                }
            }

            //now, there's a little catch here, if we are in a line with an opening peer,
            //we have to choose whether to indent to the opening peer or a little further
            //e.g.: if the line is
            //method(  self <<- a new line here should indent to the start of the self and not
            //to the opening peer.
            if (openingPeerIsInCurrentLine && openingPeerOffset < offset) {
                String fromParToCursor = document.get(openingPeerOffset, offset - openingPeerOffset);
                if (fromParToCursor.length() > 0 && fromParToCursor.charAt(0) == '(') {
                    fromParToCursor = fromParToCursor.substring(1);
                    if (!PySelection.containsOnlyWhitespaces(fromParToCursor)) {
                        final int firstCharPosition = PySelection.getFirstCharPosition(fromParToCursor);
                        openingPeerOffset += firstCharPosition;
                    }
                }
            }

            int openingPeerLineOffset = lineInformationOfOffset.getOffset();
            len = openingPeerOffset - openingPeerLineOffset;
            contents = document.get(openingPeerLineOffset, len);
        } else {
            if (!openingPeerIsInCurrentLine) {
                return new Tuple<Integer, Boolean>(-1, true);
            }

            //ok, don't indent to parenthesis level: Just add the regular indent level
            int line = document.getLineOfOffset(openingPeerOffset);
            final String indent = prefs.getIndentationString();
            contents = PySelection.getLine(document, line);
            contents = PySelection.getIndentationFromLine(contents);
            final StringBuffer sb = new StringBuffer();

            //Create the string for the indent level we want.
            for (int i = 0; i < indentAfterParWidth; i++) {
                sb.append(indent);
            }
            contents += sb.substring(0, sb.length() - 1); //we have to make it -1 (that's what the smartindent expects)
            len = contents.length();
        }
        //add more spaces for each tab
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) == '\t') {
                len += prefs.getTabWidth() - 1;
            }
        }
        return new Tuple<Integer, Boolean>(len, true);

    }

    public void setBlockSelection(boolean blockSelection) {
        this.blockSelection = blockSelection;
    }

    /**
     * Empty document (should not be written to).
     */
    IDocument EMPTY_DOCUMENT = new Document();

    private boolean isCython;

    @Override
    public String convertTabs(String cmd) {
        DocCmd newStr = new DocCmd(0, 0, cmd);
        getIndentPrefs().convertToStd(EMPTY_DOCUMENT, newStr);
        cmd = newStr.text;
        return cmd;

    }

    public void setCythonFile(boolean isCython) {
        this.isCython = isCython;
    }
}