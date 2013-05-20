package org.python.pydev.shared_core.auto_edit;

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
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.DocCmd;

/**
 * Something similar org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor.BracketInserter (but not too similar). 
 * 
 * @author Fabio Zadrozny
 */
public class AutoEditStrategyPeerHelper {

    private int linkOffset;
    private int linkExitPos;
    private int linkLen;

    /**
     * Creates a handler that will properly treat backspaces considering python code.
     */
    public static VerifyKeyListener createVerifyKeyListener(final TextViewer viewer) {
        return new VerifyKeyListener() {

            private final AutoEditStrategyPeerHelper pyPeerLinker = new AutoEditStrategyPeerHelper();

            public void verifyKey(VerifyEvent event) {
                if (!event.doit) {
                    return;
                }
                switch (event.character) {
                    case '\'':
                    case '\"':
                    case '[':
                    case '{':
                    case '(':
                        break;
                    default:
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

                            if (pyPeerLinker.perform(ps, event.character, viewer)) {
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
     */
    protected boolean perform(TextSelectionUtils ps, final char c, TextViewer viewer) {
        linkOffset = -1;
        linkExitPos = -1;
        linkLen = 0;

        boolean literal = true;
        switch (c) {
            case '\'':
            case '\"':
                break;
            case '[':
            case '{':
            case '(':
            case '<':
                literal = false;
                break;
            default:
                return false;
        }

        //        if (literal) {
        //            if (!prefs.getAutoLiterals()) {
        //                return false;
        //            }
        //        } else {
        //            if (!prefs.getAutoParentesis()) {
        //                return false;
        //            }
        //        }

        try {
            IDocument doc = ps.getDoc();

            DocCmd docCmd = new DocCmd(ps.getAbsoluteCursorOffset(), ps.getSelLength(), "" + c);
            if (literal) {
                if (!handleLiteral(doc, docCmd, ps)) {
                    return false; //not handled
                }
            } else {
                if (!handleBrackets(ps, c, doc, docCmd, viewer)) {
                    return false; //not handled
                }
            }
            if (linkOffset == -1 || linkExitPos == -1) {
                return true; //it was handled (without the link)
            }

            viewer.setSelectedRange(linkOffset, linkLen);

        } catch (Exception e) {
            Log.log(e);
        }
        return true;
    }

    private boolean handleBrackets(TextSelectionUtils ps, final char c, IDocument doc, DocCmd docCmd, TextViewer viewer)
            throws BadLocationException {
        AutoEditStrategyHelper helper = new AutoEditStrategyHelper(doc, new DocCmd(ps.getAbsoluteCursorOffset(),
                ps.getSelLength(), "" + c));
        //        if (c == '(') {
        //
        //            helper.handleOpenParens(doc, docCmd, c);
        //
        //            docCmd.doExecute(doc);
        //
        //            //Note that this is done with knowledge on how the handleParens deals with the doc command (not meant as a
        //            //general thing to apply a doc command).
        //            if (docCmd.shiftsCaret) {
        //                //Regular stuff: just shift it and don't link
        //                if (viewer != null) {
        //                    viewer.setSelectedRange(docCmd.offset + docCmd.text.length(), 0);
        //                }
        //            } else {
        //                linkOffset = docCmd.caretOffset;
        //                linkLen = 0;
        //                linkExitPos = docCmd.offset + docCmd.text.length();
        //            }
        //
        //        } else { //  [ or {
        char peer = org.python.pydev.shared_core.string.StringUtils.getPeer(c);
        if (helper.shouldClose(ps, c, peer)) {
            int offset = ps.getAbsoluteCursorOffset();
            doc.replace(offset, ps.getSelLength(),
                    org.python.pydev.shared_core.string.StringUtils.getWithClosedPeer(c));
            linkOffset = offset + 1;
            linkLen = 0;
            linkExitPos = linkOffset + linkLen + 1;
        } else {
            //No link, just add the char and set the new selected range (if possible)
            docCmd.doExecute(doc);
            if (viewer != null) {
                viewer.setSelectedRange(docCmd.offset + docCmd.text.length(), 0);
            }
        }
        //        }

        //Yes, in this situation, all cases are handled.
        return true;
    }

    /**
     * Called right after a ' or "
     * 
     * @return false if we should leave the handling to the auto-indent and true if it handled things properly here.
     */
    private boolean handleLiteral(IDocument document, DocumentCommand command, TextSelectionUtils ps)
            throws BadLocationException {
        int offset = ps.getAbsoluteCursorOffset();

        if (command.length > 0) {
            String selectedText = ps.getSelectedText();
            if (selectedText.indexOf('\r') != -1 || selectedText.indexOf('\n') != -1) {
                //we have a new line
                FastStringBuffer buf = new FastStringBuffer(selectedText.length() + 10);
                buf.appendN(command.text, 3);
                buf.append(selectedText);
                buf.appendN(command.text, 3);
                document.replace(offset, ps.getSelLength(), buf.toString());
                linkOffset = offset + 3;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 3;
            } else {
                document.replace(offset, ps.getSelLength(), command.text + selectedText + command.text);
                linkOffset = offset + 1;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 1;
            }
            return true;
        }
        char literalChar = command.text.charAt(0);

        try {
            char nextChar = ps.getCharAfterCurrentOffset();
            if (Character.isJavaIdentifierPart(nextChar)) {
                //we're just before a word (don't try to do anything in this case)
                //e.g. |var (| is cursor position)
                return false;
            }
        } catch (BadLocationException e) {
        }

        String cursorLineContents = ps.getCursorLineContents();
        if (cursorLineContents.indexOf(literalChar) == -1) {
            document.replace(offset, ps.getSelLength(), command.text + command.text);
            linkOffset = offset + 1;
            linkLen = 0;
            linkExitPos = linkOffset + linkLen + 1;
            return true;
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

                document.replace(offset, ps.getSelLength(), command.text + command.text);
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