/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_core.auto_edit;

import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.DocCmd;

/**
 * Something similar org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor.BracketInserter (but not too similar).
 *
 * @author Fabio Zadrozny
 */
public class AutoEditStrategyScopeCreationHelper {

    private int linkOffset;
    private int linkExitPos;
    private int linkLen;

    public static interface IScopeCreatingCharsProvider {

        int CLOSE_SCOPE = 1; //
        int CLOSE_SCOPE_IF_SELECTION = 2;
        int CLOSE_SCOPE_NO = 3;

        int getCharactersThatCreateScope(String contentType, char c);

        /**
         * A list with the start/end sequences that belong to multi-line creation.
         */
        Set<Tuple<String, String>> getMultiLineSequences();
    }

    /**
     * Creates a handler that will properly treat peers.
     */
    public static VerifyKeyListener createVerifyKeyListener(final TextViewer viewer,
            final IScopeCreatingCharsProvider provider) {
        return new VerifyKeyListener() {

            private final AutoEditStrategyScopeCreationHelper scopeHelper = new AutoEditStrategyScopeCreationHelper();

            public void verifyKey(VerifyEvent event) {
                if (!event.doit || event.character == '\0') {
                    return;
                }
                if (viewer != null && viewer.isEditable()) {
                    boolean blockSelection = false;
                    try {
                        blockSelection = viewer.getTextWidget().getBlockSelection();
                    } catch (Throwable e) {
                        //that's OK (only available in eclipse 3.5)
                    }
                    if (!blockSelection) {
                        ISelection selection = viewer.getSelection();
                        if (selection instanceof ITextSelection) {

                            //Don't bother in getting the indent prefs from the editor: the default indent prefs are
                            //always global for the settings we want.
                            TextSelectionUtils ps = new TextSelectionUtils(viewer.getDocument(),
                                    (ITextSelection) selection);

                            int absoluteCursorOffset = ps.getAbsoluteCursorOffset();
                            String contentType = AutoEditStrategyHelper.getContentType(ps.getDoc(),
                                    absoluteCursorOffset, false);

                            int closeScope = provider.getCharactersThatCreateScope(contentType, event.character);
                            switch (closeScope) {
                                case IScopeCreatingCharsProvider.CLOSE_SCOPE:
                                    break; //keep on going in this function

                                case IScopeCreatingCharsProvider.CLOSE_SCOPE_NO:
                                    return;

                                case IScopeCreatingCharsProvider.CLOSE_SCOPE_IF_SELECTION:
                                    if (ps.getSelLength() == 0) {
                                        return;
                                    }
                                    break;
                            }
                            if (scopeHelper.perform(ps, event.character, viewer, provider)) {
                                event.doit = false;
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * @param ps
     * @param provider
     */
    public boolean perform(TextSelectionUtils ps, final char c, TextViewer viewer, IScopeCreatingCharsProvider provider) {
        linkOffset = -1;
        linkExitPos = -1;
        linkLen = 0;

        try {
            IDocument doc = ps.getDoc();

            DocCmd docCmd = new DocCmd(ps.getAbsoluteCursorOffset(), ps.getSelLength(), Character.toString(c));
            if (!handleScopeCreationChar(doc, docCmd, ps, provider, c)) {
                return false; //not handled
            }
            if (linkOffset == -1 || linkExitPos == -1) {
                return true; //it was handled (without the link)
            }

            if (viewer != null) {
                viewer.setSelectedRange(linkOffset, linkLen);
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return true;
    }

    /**
     * Called right after a ' or "
     * @param provider
     *
     * @return false if we should leave the handling to the auto-indent and true if it handled things properly here.
     */
    private boolean handleScopeCreationChar(IDocument document, DocumentCommand command, TextSelectionUtils ps,
            IScopeCreatingCharsProvider provider, char c)
            throws BadLocationException {
        int offset = ps.getAbsoluteCursorOffset();

        Set<Tuple<String, String>> multiLineSequences = provider.getMultiLineSequences();
        Tuple<String, String> foundMultiLine = null;
        for (Tuple<String, String> tuple : multiLineSequences) {
            if (tuple.o1.length() > 0 && tuple.o1.charAt(0) == c) {
                foundMultiLine = tuple;
                break;
            }
        }
        if (command.length > 0) {

            String selectedText = ps.getSelectedText();
            if (selectedText.indexOf('\r') != -1 || selectedText.indexOf('\n') != -1) {
                if (foundMultiLine != null) {
                    //we have a new line in the selection
                    FastStringBuffer buf = new FastStringBuffer(selectedText.length() + 10);
                    buf.append(foundMultiLine.o1);
                    buf.append(selectedText);
                    buf.append(foundMultiLine.o2);
                    document.replace(offset, ps.getSelLength(), buf.toString());
                    linkOffset = offset + foundMultiLine.o1.length();
                    linkLen = selectedText.length();
                    linkExitPos = linkOffset + linkLen + foundMultiLine.o2.length();
                } else {
                    // Not handling multi-line if there's no way to create it at this time.
                    return false;
                }
            } else {
                document.replace(offset, ps.getSelLength(), getReplacement(command, foundMultiLine, selectedText));
                linkOffset = offset + 1;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 1;
            }
            return true;
        }

        try {
            char nextChar = ps.getCharAtCurrentOffset();
            if (Character.isJavaIdentifierPart(nextChar)) {
                //we're just before a word (don't try to do anything in this case)
                //e.g. |var (| is cursor position)
                return false;
            }
        } catch (BadLocationException e) {
        }

        String cursorLineContents = ps.getCursorLineContents();
        if (cursorLineContents.indexOf(c) == -1) {
            document.replace(offset, ps.getSelLength(), getReplacement(command, foundMultiLine, ""));
            linkOffset = offset + 1;
            linkLen = 0;
            linkExitPos = linkOffset + linkLen + 1;
            return true;
        } else if (foundMultiLine != null && command.text.length() == 1) {
            int length = foundMultiLine.o1.length();
            if (length > 1) {
                if (foundMultiLine.o1.charAt(length - 1) == command.text.charAt(0)) {
                    //if the last char of the multiline is equal to the char being added
                    IDocument doc = ps.getDoc();
                    int currOffset = ps.getAbsoluteCursorOffset();
                    String string = doc.get(currOffset - (length - 1), length - 1);
                    //Check if the previous chars in the document match the start of the multiline (without the last char as that's what being added)
                    //If all that matches: close the scope automatically.
                    if (string.equals(foundMultiLine.o1.substring(0, length - 1))) {
                        FastStringBuffer buf = new FastStringBuffer(10);
                        buf.append(c);
                        buf.append(foundMultiLine.o2);
                        document.replace(offset, 0, buf.toString());
                        linkOffset = offset + 1;
                        linkLen = 0;
                        linkExitPos = linkOffset + linkLen + foundMultiLine.o2.length();
                        return true;
                    }
                }
            }

        }

        boolean balanced = isLiteralBalanced(cursorLineContents);

        Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(c);

        int matchesBefore = beforeAndAfterMatchingChars.o1.length();
        int matchesAfter = beforeAndAfterMatchingChars.o2.length();

        boolean hasMatchesBefore = matchesBefore != 0;
        boolean hasMatchesAfter = matchesAfter != 0;

        if (!hasMatchesBefore && !hasMatchesAfter) {
            //if it's not balanced, this char would be the closing char.
            if (balanced) {

                document.replace(offset, ps.getSelLength(), getReplacement(command, foundMultiLine, ""));
                linkOffset = offset + 1;
                linkLen = 0;
                linkExitPos = linkOffset + linkLen + 1;
                return true;
            }
        } else {
            //we're right after or before a " or '
            return false;
        }
        return false;
    }

    private String getReplacement(DocumentCommand command, Tuple<String, String> found, String selectedText) {
        String replacement;
        if (found != null && found.o1.equals(command.text)) {
            replacement = found.o1 + selectedText + found.o2;

        } else {
            if (command.text.length() == 1) {
                char peer;
                char char0 = command.text.charAt(0);
                try {
                    peer = StringUtils.getPeer(char0);
                } catch (Exception e) {
                    peer = char0;
                }
                replacement = char0 + selectedText + peer;
            } else {
                replacement = command.text + selectedText + command.text;
            }

        }
        return replacement;
    }

    /**
     * @return true if the passed string has balanced ' and "
     */
    private boolean isLiteralBalanced(String cursorLineContents) {
        return true;
    }

    /**
     * In default namespace (used for testing)
     */

    int getLinkLen() {
        return linkLen;
    }

    int getLinkExitPos() {
        return linkExitPos;
    }

    int getLinkOffset() {
        return linkOffset;
    }

}