/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc and Fabio Zadrozny. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: Fabio Zadrozny
 * Created: July 2004
 */
package org.python.pydev.shared_core.auto_edit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Fabio Zadrozny
 * 
 * Makes a backspace happen...
 * 
 * We can:
 * - go to the indentation from some uncommented previous line (if we
 *   only have whitespaces in the current line).
 * - erase all whitespace characters until we find some character.
 * - erase a single character.
 */
public class AutoEditStrategyBackspaceHelper {

    private int dontEraseMoreThan = -1;
    private IIndentationStringProvider indentationStringProvider;

    /**
     * Returns the position of the last non whitespace char in the current line.
     * @param doc
     * @param cursorOffset
     * @return position of the last character of the line (returned as an absolute
     *            offset)
     * 
     * @throws BadLocationException
     */
    protected int getLastCharPosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
        region = doc.getLineInformationOfOffset(cursorOffset);
        int offset = region.getOffset();
        String src = doc.get(offset, region.getLength());

        int i = src.length();
        boolean breaked = false;
        while (i > 0) {
            i--;
            //we have to break if we find a character that is not a whitespace or a tab.
            if (Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t') {
                breaked = true;
                break;
            }
        }
        if (!breaked) {
            i--;
        }
        return (offset + i);
    }

    public void perform(TextSelectionUtils ps) {
        // Perform the action
        try {
            ITextSelection textSelection = ps.getTextSelection();

            if (textSelection.getLength() != 0) {
                eraseSelection(ps);
                return;
            }

            int lastCharPosition = getLastCharPosition(ps.getDoc(), ps.getLineOffset());

            int cursorOffset = textSelection.getOffset();

            IRegion lastCharRegion = ps.getDoc().getLineInformationOfOffset(lastCharPosition + 1);
            //System.out.println("cursorOffset: "+ cursorOffset);
            //System.out.println("lastCharPosition: "+lastCharPosition);
            //System.out.println("lastCharRegion.getOffset(): "
            // +lastCharRegion.getOffset());
            //          IRegion cursorRegion =
            // ps.doc.getLineInformationOfOffset(cursorOffset);

            if (cursorOffset == lastCharRegion.getOffset()) {
                //System.out.println("We are in the beginning of the line.");
                //in this situation, we are in the first character of the
                // line...
                //so, we have to get the end of the other line and delete it.
                if (cursorOffset != 0) {
                    // the first line.
                    eraseLineDelimiter(ps);
                }
            } else if (cursorOffset <= lastCharPosition) {
                //System.out.println("cursorOffset <= lastCharPosition");
                //this situation is:
                //    |a (delete to previous indentation - considers cursor
                // position)
                //or
                //    as | as (delete single char)
                //or
                //  | a (delete to previous indentation - considers cursor
                // position)
                //so, we have to treat it carefully
                eraseToPreviousIndentation(ps, false, lastCharRegion);
            } else if (lastCharRegion.getOffset() == lastCharPosition + 1) {
                //System.out.println("Only whitespaces in the line.");
                //in this situation, this line only has whitespaces,
                //so, we have to erase depending on the previous indentation.
                eraseToPreviousIndentation(ps, true, lastCharRegion);
            } else {

                if (cursorOffset - lastCharPosition == 1) {
                    //System.out.println("Erase single char.");
                    //last char and cursor are in the same line.
                    //this situation is:
                    //    a|
                    eraseSingleChar(ps);

                } else if (cursorOffset - lastCharPosition > 1) {
                    //this situation is:
                    //    a |
                    //System.out.println("Erase until last char is found.");
                    eraseUntilLastChar(ps, lastCharPosition);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @param ps
     * @param hasOnlyWhitespaces
     * @param lastCharRegion 
     * @throws BadLocationException
     */
    private void eraseToPreviousIndentation(TextSelectionUtils ps, boolean hasOnlyWhitespaces, IRegion lastCharRegion)
            throws BadLocationException {
        String lineContentsToCursor = ps.getLineContentsToCursor();
        if (hasOnlyWhitespaces) {
            //System.out.println("only whitespaces");
            eraseToIndentation(ps, lineContentsToCursor);
        } else {
            //System.out.println("not only whitespaces");
            //this situation is:
            //    |a (delete to previous indentation - considers cursor position)
            //
            //or
            //
            //    as | as (delete single char)
            //
            //so, we have to treat it carefully
            //TODO: use the conditions above and not just erase a single
            // char.

            if (TextSelectionUtils.containsOnlyWhitespaces(lineContentsToCursor)) {
                eraseToIndentation(ps, lineContentsToCursor);

            } else {
                eraseSingleChar(ps);
            }
        }
    }

    private void eraseSingleChar(TextSelectionUtils ps) throws BadLocationException {

        ITextSelection textSelection = ps.getTextSelection();

        int replaceLength = 1;
        int replaceOffset = textSelection.getOffset() - replaceLength;
        IDocument doc = ps.getDoc();
        String contentType = doc.getContentType(replaceOffset);

        if (replaceOffset >= 0 && replaceOffset + replaceLength < doc.getLength()) {
            char c = doc.getChar(replaceOffset);
            if (c == '(' || c == '[' || c == '{' || c == '<') {
                //When removing a (, check if we have to delete the corresponding ) too.
                char peer = StringUtils.getPeer(c);
                if (replaceOffset + replaceLength < doc.getLength()) {
                    char c2 = doc.getChar(replaceOffset + 1);
                    if (c2 == peer) {
                        //Ok, there's a closing one right next to it, now, what we have to do is
                        //check if the user was actually removing that one because there's an opening
                        //one without a match.
                        //To do that, we go backwards in the document searching for an opening match and then
                        //search its match. If it's found, it means we can delete both, otherwise, this
                        //delete will make things correct.

                        //Create a matcher only matching this char

                        AutoEditPairMatcher pairMatcher = new AutoEditPairMatcher(
                                new char[] { c, peer }, contentType);
                        int openingPeerOffset = pairMatcher.searchForAnyOpeningPeer(replaceOffset, doc);
                        if (openingPeerOffset == -1) {
                            replaceLength += 1;
                        } else {
                            int closingPeerOffset = pairMatcher.searchForClosingPeer(openingPeerOffset + 1, c, peer,
                                    doc);
                            if (closingPeerOffset != -1) {
                                //we have a match, so, things are balanced and we can delete the next
                                replaceLength += 1;
                            }
                        }
                    }
                }

            } else if (c == '\'' || c == '"') {
                //when removing a ' or ", check if we have to delete another ' or " too.
                Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(c);
                int matchesBefore = beforeAndAfterMatchingChars.o1.length();
                int matchesAfter = beforeAndAfterMatchingChars.o2.length();
                if (matchesBefore == 1 && matchesBefore == matchesAfter) {
                    replaceLength += 1;
                }
            }
        }
        makeDelete(doc, replaceOffset, replaceLength);
    }

    /**
     * 
     * @param ps
     * @throws BadLocationException
     */
    private void eraseLineDelimiter(TextSelectionUtils ps) throws BadLocationException {

        ITextSelection textSelection = ps.getTextSelection();

        int length = TextSelectionUtils.getDelimiter(ps.getDoc()).length();
        int offset = textSelection.getOffset() - length;

        //System.out.println("Replacing offset: "+(offset) +" lenght: "+
        // (length));
        makeDelete(ps.getDoc(), offset, length);
    }

    /**
     * 
     * @param ps
     * @throws BadLocationException
     */
    private void eraseSelection(TextSelectionUtils ps) throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();

        makeDelete(ps.getDoc(), textSelection.getOffset(), textSelection.getLength());
    }

    /**
     * @param ps
     * @param lastCharPosition
     * @throws BadLocationException
     */
    private void eraseUntilLastChar(TextSelectionUtils ps, int lastCharPosition) throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();
        int cursorOffset = textSelection.getOffset();

        int offset = lastCharPosition + 1;
        int length = cursorOffset - lastCharPosition - 1;
        //System.out.println("Replacing offset: "+(offset) +" lenght: "+
        // (length));
        makeDelete(ps.getDoc(), offset, length);
    }

    /**
     * TODO: Make use of the indentation gotten previously. This implementation
     * just uses the indentation string and erases the number of chars from it.
     * 
     * @param ps
     * @param indentation this is in number of characters.
     * @throws BadLocationException
     */
    private void eraseToIndentation(TextSelectionUtils ps, String lineContentsToCursor) throws BadLocationException {
        final int cursorOffset = ps.getAbsoluteCursorOffset();
        final int lineContentsToCursorLen = lineContentsToCursor.length();

        if (lineContentsToCursorLen > 0) {
            char c = lineContentsToCursor.charAt(lineContentsToCursorLen - 1);
            if (c == '\t') {
                eraseSingleChar(ps);
                return;
            }
        }

        String indentationString = this.indentationStringProvider.getIndentationString();

        int replaceLength;
        int replaceOffset;

        final int indentationLength = indentationString.length();
        final int modLen = lineContentsToCursorLen % indentationLength;

        if (modLen == 0) {
            replaceOffset = cursorOffset - indentationLength;
            replaceLength = indentationLength;
        } else {
            replaceOffset = cursorOffset - modLen;
            replaceLength = modLen;
        }

        IDocument doc = ps.getDoc();
        //        final int cursorLine = ps.getCursorLine();
        //        if (cursorLine > 0) {
        //            IRegion prevLineInfo = doc.getLineInformation(cursorLine - 1);
        //            int prevLineEndOffset = prevLineInfo.getOffset() + prevLineInfo.getLength();
        //            Tuple<Integer, Boolean> tup = PyAutoIndentStrategy.determineSmartIndent(prevLineEndOffset, doc, prefs);
        //            Integer previousContextSmartIndent = tup.o1;
        //            if (previousContextSmartIndent > 0 && lineContentsToCursorLen > previousContextSmartIndent) {
        //                int initialLineOffset = cursorOffset - lineContentsToCursorLen;
        //                if (replaceOffset < initialLineOffset + previousContextSmartIndent) {
        //                    int newReplaceOffset = initialLineOffset + previousContextSmartIndent + 1;
        //                    if (newReplaceOffset != cursorOffset) {
        //                        replaceOffset = newReplaceOffset;
        //                        replaceLength = cursorOffset - replaceOffset;
        //                    }
        //                }
        //            }
        //        }

        //now, check what we're actually removing here... we can only remove chars if they are the
        //same, so, if we have a replace for '\t ', we should only remove the ' ', and not the '\t'
        if (replaceLength > 1) {
            String strToReplace = doc.get(replaceOffset, replaceLength);
            char prev = 0;
            for (int i = strToReplace.length() - 1; i >= 0; i--) {
                char c = strToReplace.charAt(i);
                if (prev != 0) {
                    if (c != prev) {
                        replaceOffset += (i + 1);
                        replaceLength -= (i + 1);
                        break;
                    }
                }
                prev = c;
            }
        }

        makeDelete(doc, replaceOffset, replaceLength);
    }

    private void makeDelete(IDocument doc, int replaceOffset, int replaceLength) throws BadLocationException {
        if (replaceOffset < dontEraseMoreThan) {
            int delta = dontEraseMoreThan - replaceOffset;
            replaceOffset = dontEraseMoreThan;
            replaceLength -= delta;
            if (replaceLength <= 0) {
                return;
            }
        }
        doc.replace(replaceOffset, replaceLength, "");
    }

    public void setDontEraseMoreThan(int offset) {
        this.dontEraseMoreThan = offset;
    }

    /**
     * Creates a handler that will properly treat backspaces considering python code.
     */
    public static VerifyKeyListener createVerifyKeyListener(final TextViewer viewer,
            final IIndentationStringProvider indentationStringProvider) {
        return new VerifyKeyListener() {

            public void verifyKey(VerifyEvent event) {
                if ((event.doit && event.character == SWT.BS && event.stateMask == 0 && viewer != null && viewer
                        .isEditable())) { //isBackspace
                    boolean blockSelection = false;
                    try {
                        blockSelection = viewer.getTextWidget().getBlockSelection();
                    } catch (Throwable e) {
                        //that's OK (only available in eclipse 3.5)
                    }
                    if (!blockSelection) {
                        ISelection selection = viewer.getSelection();
                        if (selection instanceof ITextSelection) {
                            //Only do our custom backspace if we're not in block selection mode.
                            AutoEditStrategyBackspaceHelper pyBackspace = new AutoEditStrategyBackspaceHelper();
                            pyBackspace.setIndentationStringProvider(indentationStringProvider);
                            TextSelectionUtils ps = new TextSelectionUtils(viewer.getDocument(),
                                    (ITextSelection) selection);
                            pyBackspace.perform(ps);
                            event.doit = false;
                        }
                    }
                }
            }
        };
    }

    protected void setIndentationStringProvider(IIndentationStringProvider indentationStringProvider) {
        this.indentationStringProvider = indentationStringProvider;
    }

}