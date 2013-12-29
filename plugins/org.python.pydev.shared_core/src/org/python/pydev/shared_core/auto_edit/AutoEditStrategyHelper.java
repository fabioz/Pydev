/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.auto_edit;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TypedPosition;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.partitioner.PartitionCodeReader;
import org.python.pydev.shared_core.partitioner.PartitionMerger;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.NoPeerAvailableException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class AutoEditStrategyHelper {

    public final IDocument document;
    public final DocumentCommand command;
    public final char c;

    public static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']', '<', '>' };
    public static final char[] CLOSING_BRACKETS = { '}', ')', ']', '>' };

    public AutoEditStrategyHelper(IDocument document, DocumentCommand command) {
        this.document = document;
        this.command = command;

        if (command.text.length() == 1) {
            c = command.text.charAt(0);
        } else {
            c = '\0';
        }
    }

    private AutoEditPairMatcher getMatcher() {
        return new AutoEditPairMatcher(BRACKETS, getContentType(document, command)); //work in the current content type.
    }

    /**
     * Called right after a ' or "
     */
    public void handleAutoClose(IDocument document, DocumentCommand command, char start, char end) {
        TextSelectionUtils ps = new TextSelectionUtils(document, new TextSelection(document, command.offset,
                command.length));

        try {
            char nextChar = ps.getCharAfterCurrentOffset();
            if (Character.isJavaIdentifierPart(nextChar)) {
                //we're just before a word (don't try to do anything in this case)
                //e.g. |var (| is cursor position)
                return;
            }
        } catch (BadLocationException e) {
        }

        command.text = Character.toString(start) + Character.toString(end);
        command.shiftsCaret = false;
        command.caretOffset = command.offset + 1;
    }

    public void handleAutoSkip(IDocument document, DocumentCommand command, char c) {
        TextSelectionUtils ps = new TextSelectionUtils(document, new TextSelection(document, command.offset,
                command.length));

        Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(c);

        //Reminder: int matchesBefore = beforeAndAfterMatchingChars.o1.length();
        int matchesAfter = beforeAndAfterMatchingChars.o2.length();

        if (matchesAfter == 1) {
            //just walk the caret
            command.text = "";
            command.shiftsCaret = false;
            command.caretOffset = command.offset + 1;
        }
    }

    public void handleCloseParens(IDocument document, DocumentCommand command, char c) {
        // you can only do the replacement if the next character already there is what the user is trying to input

        try {
            if (command.offset < document.getLength()
                    && document.get(command.offset, 1).equals(command.text)) {
                // the following searches through each of the end braces and
                // sees if the command has one of them

                boolean found = false;
                for (int i = 1; i <= BRACKETS.length && !found; i += 2) {
                    char b = BRACKETS[i];
                    if (b == c) {
                        found = true;
                        performPairReplacement(document, command);
                    }
                }
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    private void performPairReplacement(IDocument document, DocumentCommand command) throws BadLocationException {
        boolean skipChar = canSkipCloseParenthesis(document, command);
        if (skipChar) {
            //if we have the same number of peers, we want to eat the char
            command.text = "";
            command.caretOffset = command.offset + 1;
        }
    }

    /**
     * @return true if we should skip a ), ] or }
     */
    public boolean canSkipCloseParenthesis(IDocument document, DocumentCommand command) throws BadLocationException {
        TextSelectionUtils ps = new TextSelectionUtils(document, command.offset);

        char c = ps.getCharAtCurrentOffset();

        try {
            char peer = StringUtils.getPeer(c);

            String contentType = getContentType(document, command);
            String doc = getPartsWithPartition(document, contentType);
            int chars = StringUtils.countChars(c, doc);
            int peers = StringUtils.countChars(peer, doc);

            boolean skipChar = chars == peers;
            return skipChar;
        } catch (NoPeerAvailableException e) {
            return false;
        }
    }

    private String getPartsWithPartition(IDocument document, String contentType) {
        Position[] positions = PartitionCodeReader.getDocumentTypedPositions(document, contentType);
        int total = 0;
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            total += position.length;
        }

        List<TypedPosition> sortAndMergePositions = PartitionMerger.sortAndMergePositions(positions,
                document.getLength());
        FastStringBuffer buf = new FastStringBuffer(total + 5);

        for (TypedPosition typedPosition : sortAndMergePositions) {
            try {
                String string = document.get(typedPosition.getOffset(), typedPosition.getLength());
                buf.append(string);
            } catch (BadLocationException e) {
                Log.log(e);
            }
        }
        return buf.toString();
    }

    public static String getContentType(IDocument document, DocumentCommand command) {
        return getContentType(document, command.offset, true);
    }

    public static String getContentType(IDocument document, int offset, boolean preferOpen) {
        IDocumentExtension3 extension = (IDocumentExtension3) document;
        String contentType;
        try {
            contentType = extension.getContentType(IDocumentExtension3.DEFAULT_PARTITIONING, offset,
                    preferOpen);
        } catch (BadLocationException e) {
            //Don't log this one
            contentType = IDocument.DEFAULT_CONTENT_TYPE;

        } catch (Exception e) {
            Log.log(e);
            contentType = IDocument.DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    public void handleOpenParens(IDocument document2, DocumentCommand command2, char c2) {
        try {
            TextSelectionUtils ps = new TextSelectionUtils(document, command.offset);
            char peer = StringUtils.getPeer(c);
            if (shouldClose(ps, c, peer)) {
                command.shiftsCaret = false;
                command.text = c + "" + peer;
                command.caretOffset = command.offset + 1;
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    /**
     * @return true if we should close the opening pair (parameter c) and false if we shouldn't
     */
    public boolean shouldClose(TextSelectionUtils ps, char c, char peer) throws BadLocationException {
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

        AutoEditPairMatcher matcher = getMatcher();
        int closingPeerLine;
        int closingPeerFoundAtOffset = ps.getAbsoluteCursorOffset(); //start to search at the current position

        int searchUntilLine = ps.getCursorLine() + 5; //search at most 5 lines
        do {
            closingPeerFoundAtOffset = matcher.searchForClosingPeer(closingPeerFoundAtOffset, c, peer, ps.getDoc());
            if (closingPeerFoundAtOffset == -1) {
                //no more closing peers there, ok to go
                return true;
            }

            //the +1 is needed because we match closing ones that are right before the current cursor (and we need to walk)
            closingPeerFoundAtOffset++;
            IRegion match = matcher.match(ps.getDoc(), closingPeerFoundAtOffset);
            if (match == null) {
                //we don't have a match for a close, so, this open is that match.
                return false;
            }

            try {
                closingPeerLine = ps.getDoc().getLineOfOffset(closingPeerFoundAtOffset);
            } catch (Exception e) {
                break;
            }
        } while (searchUntilLine > closingPeerLine);

        return true;
    }

    // Does not work because we cannot differentiate from a delete or backspace!
    //    public void handleDelete(IDocument document, DocumentCommand command, char c, String regularIndent) {
    //        try {
    //            AutoEditStrategyBackspaceHelper helper = new AutoEditStrategyBackspaceHelper();
    //            TextSelectionUtils ts = new TextSelectionUtils(document, command.offset);
    //            helper.handleDelete(ts, getContentType(document, command), command, regularIndent);
    //        } catch (BadLocationException e) {
    //            Log.log(e);
    //        }
    //    }

    public void handleNewLine(IDocument document, DocumentCommand command, char c, String regularIndent) {
        AutoEditStrategyNewLineHelper helper = new AutoEditStrategyNewLineHelper();
        TextSelectionUtils ts = new TextSelectionUtils(document, command.offset);
        helper.handleNewLine(ts, getContentType(document, command), command, regularIndent);
    }

}
