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
                if (!event.doit) {
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
                            if (absoluteCursorOffset > 0) {
                                absoluteCursorOffset--; //We want to get the 'current' open partition.
                            }
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

        if (command.length > 0) {
            String selectedText = ps.getSelectedText();
            if (selectedText.indexOf('\r') != -1 || selectedText.indexOf('\n') != -1) {
                Set<Tuple<String, String>> multiLineSequences = provider.getMultiLineSequences();
                Tuple<String, String> found = null;
                for (Tuple<String, String> tuple : multiLineSequences) {
                    if (tuple.o1.length() > 0 && tuple.o1.charAt(0) == c) {
                        found = tuple;
                        break;
                    }
                }
                if (found != null) {
                    //we have a new line in the selection
                    FastStringBuffer buf = new FastStringBuffer(selectedText.length() + 10);
                    buf.append(found.o1);
                    buf.append(selectedText);
                    buf.append(found.o2);
                    document.replace(offset, ps.getSelLength(), buf.toString());
                    linkOffset = offset + found.o1.length();
                    linkLen = selectedText.length();
                    linkExitPos = linkOffset + linkLen + found.o2.length();
                } else {
                    // Not handling multi-line if there's no way to create it at this time.
                    return false;
                }
            } else {
                document.replace(offset, ps.getSelLength(), command.text + selectedText + command.text);
                linkOffset = offset + 1;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 1;
            }
            return true;
        }

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
        if (cursorLineContents.indexOf(c) == -1) {
            document.replace(offset, ps.getSelLength(), command.text + command.text);
            linkOffset = offset + 1;
            linkLen = 0;
            linkExitPos = linkOffset + linkLen + 1;
            return true;
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