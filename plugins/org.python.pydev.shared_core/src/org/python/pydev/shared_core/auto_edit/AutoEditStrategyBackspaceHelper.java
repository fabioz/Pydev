/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.auto_edit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/*default*/class AutoEditStrategyBackspaceHelper {

    public void handleDelete(TextSelectionUtils ps, String contentType, DocumentCommand command,
            final String indentationString)
            throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();

        final int replaceLength = command.length;

        if (replaceLength != 1) {
            return;
        }
        final IDocument doc = ps.getDoc();

        final int cursorOffset = textSelection.getOffset();
        int lastCharPosition = getLastCharPosition(ps.getDoc(), ps.getLineOffset());
        IRegion lastCharRegion = ps.getDoc().getLineInformationOfOffset(lastCharPosition + 1);

        if (cursorOffset == lastCharRegion.getOffset()) {
            //System.out.println("We are in the beginning of the line.");
            //in this situation, we are in the first character of the
            // line...
            //so, we have to get the end of the other line and delete it.
            //All set: regular delete.
            //            if (cursorOffset != 0) //we only want to erase if we are not in
            //                                   // the first line.
            //                eraseLineDelimiter(ps);
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
            eraseToPreviousIndentation(ps, false, lastCharRegion, indentationString, command);
        } else if (lastCharRegion.getOffset() == lastCharPosition + 1) {
            //System.out.println("Only whitespaces in the line.");
            //in this situation, this line only has whitespaces,
            //so, we have to erase depending on the previous indentation.
            eraseToPreviousIndentation(ps, true, lastCharRegion, indentationString, command);
        } else {

            if (cursorOffset - lastCharPosition == 1) {
                //System.out.println("Erase single char.");
                //last char and cursor are in the same line.
                //this situation is:
                //    a|
                eraseSingleChar(ps, contentType, command, replaceLength, doc, cursorOffset);

            } else if (cursorOffset - lastCharPosition > 1) {
                //this situation is:
                //    a |
                //System.out.println("Erase until last char is found.");
                eraseUntilLastChar(ps, lastCharPosition, command);
            }
        }
    }

    private void eraseToPreviousIndentation(TextSelectionUtils ps, boolean hasOnlyWhitespaces, IRegion lastCharRegion,
            String indentationString, DocumentCommand command)
            throws BadLocationException {
        String lineContentsToCursor = ps.getLineContentsToCursor();
        if (hasOnlyWhitespaces) {
            //System.out.println("only whitespaces");
            eraseToIndentation(ps, lineContentsToCursor, indentationString, command);
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
                eraseToIndentation(ps, lineContentsToCursor, indentationString, command);

            } else {
                //Nothing to do: default is Ok.
                //eraseSingleChar(ps);
            }
        }
    }

    /**
     * @param ps
     * @param lastCharPosition
     * @param command 
     * @throws BadLocationException
     */
    private void eraseUntilLastChar(TextSelectionUtils ps, int lastCharPosition, DocumentCommand command)
            throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();
        int cursorOffset = textSelection.getOffset();

        int offset = lastCharPosition + 1;
        int length = cursorOffset - lastCharPosition - 1;
        //System.out.println("Replacing offset: "+(offset) +" lenght: "+
        // (length));
        makeDelete(ps.getDoc(), offset, length, command);
    }

    private void makeDelete(IDocument doc, int offset, int length, DocumentCommand command) {
        command.offset = offset;
        command.length = length;
        //System.out.println("here");
        //System.out.println(command.offset);
        //System.out.println(command.length);
        //System.out.println(command.caretOffset);
    }

    /**
     * TODO: Make use of the indentation gotten previously. This implementation
     * just uses the indentation string and erases the number of chars from it.
     * 
     * @param ps
     * @param command 
     * @param indentation this is in number of characters.
     * @throws BadLocationException
     */
    private void eraseToIndentation(TextSelectionUtils ps, String lineContentsToCursor, String indentationString,
            DocumentCommand command)
            throws BadLocationException {
        final int cursorOffset = ps.getAbsoluteCursorOffset();
        final int cursorLine = ps.getCursorLine();
        final int lineContentsToCursorLen = lineContentsToCursor.length();

        if (lineContentsToCursorLen > 0) {
            char c = lineContentsToCursor.charAt(lineContentsToCursorLen - 1);
            if (c == '\t') {
                //eraseSingleChar(ps); -- just use the default
                return;
            }
        }

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
        if (cursorLine > 0) {
            //IRegion prevLineInfo = doc.getLineInformation(cursorLine - 1);
            //int prevLineEndOffset = prevLineInfo.getOffset() + prevLineInfo.getLength();
            //Tuple<Integer, Boolean> tup = PyAutoIndentStrategy.determineSmartIndent(prevLineEndOffset, doc, prefs);
            //Integer previousContextSmartIndent = tup.o1;
            //if (previousContextSmartIndent > 0 && lineContentsToCursorLen > previousContextSmartIndent) {
            //    int initialLineOffset = cursorOffset - lineContentsToCursorLen;
            //    if (replaceOffset < initialLineOffset + previousContextSmartIndent) {
            //        int newReplaceOffset = initialLineOffset + previousContextSmartIndent + 1;
            //        if (newReplaceOffset != cursorOffset) {
            //            replaceOffset = newReplaceOffset;
            //            replaceLength = cursorOffset - replaceOffset;
            //        }
            //    }
            //}
        }

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

        makeDelete(doc, replaceOffset, replaceLength, command);
    }

    private void eraseSingleChar(TextSelectionUtils ps, String contentType, DocumentCommand command,
            final int replaceLength, final IDocument doc, final int cursorOffset) throws BadLocationException {
        if (cursorOffset >= 0 && cursorOffset + replaceLength < doc.getLength()) {
            char c = doc.getChar(cursorOffset);
            if (c == '(' || c == '[' || c == '{' || c == '<') {
                //When removing a (, check if we have to delete the corresponding ) too.
                char peer = org.python.pydev.shared_core.string.StringUtils.getPeer(c);
                if (cursorOffset + replaceLength < doc.getLength()) {
                    char c2 = doc.getChar(cursorOffset + 1);
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
                        int openingPeerOffset = pairMatcher.searchForAnyOpeningPeer(cursorOffset + 1, doc);
                        if (openingPeerOffset == -1) {
                            command.length += 1;
                        } else {
                            int closingPeerOffset = pairMatcher.searchForClosingPeer(openingPeerOffset, c, peer,
                                    doc);
                            if (closingPeerOffset != -1) {
                                //we have a match, so, things are balanced and we can delete the next
                                command.length += 1;
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
                    command.length += 1;
                }
            }
        }
    }

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
}
