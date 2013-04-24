/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.auto_edit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/*default*/class AutoEditStrategyBackspaceHelper {

    public void handleDelete(TextSelectionUtils ps, String contentType, DocumentCommand command)
            throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();

        final int replaceLength = command.length;
        if (replaceLength != 1) {
            return;
        }
        int replaceOffset = textSelection.getOffset();
        IDocument doc = ps.getDoc();

        if (replaceOffset >= 0 && replaceOffset + replaceLength < doc.getLength()) {
            char c = doc.getChar(replaceOffset);
            if (c == '(' || c == '[' || c == '{' || c == '<') {
                //When removing a (, check if we have to delete the corresponding ) too.
                char peer = org.python.pydev.shared_core.string.StringUtils.getPeer(c);
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
                        int openingPeerOffset = pairMatcher.searchForAnyOpeningPeer(replaceOffset + 1, doc);
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

}
