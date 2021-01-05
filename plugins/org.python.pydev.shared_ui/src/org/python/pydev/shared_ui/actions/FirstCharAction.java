package org.python.pydev.shared_ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

/**
 * @author Fabio Zadrozny
 */
public class FirstCharAction extends BaseAction {

    protected SourceViewer viewer;

    /**
     * Run to the first char (other than whitespaces) or to the real first char.
     */
    @Override
    public void run(IAction action) {

        try {
            ITextEditor textEditor = getTextEditor();
            IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();

            perform(doc, selection);
        } catch (Exception e) {
            beep(e);
        }
    }

    private void perform(IDocument doc, ITextSelection selection) {
        boolean isAtFirstChar = isAtFirstVisibleChar(doc, selection.getOffset());
        if (!isAtFirstChar) {
            gotoFirstVisibleChar(doc, selection.getOffset());
        } else {
            gotoFirstChar(doc, selection.getOffset());
        }
    }

    /**
     * Goes to first char of the line.
     * @param doc
     * @param cursorOffset
     */
    protected void gotoFirstChar(IDocument doc, int cursorOffset) {
        try {
            IRegion region = doc.getLineInformationOfOffset(cursorOffset);
            int offset = region.getOffset();
            setCaretPosition(offset);
        } catch (BadLocationException e) {
            beep(e);
        }
    }

    /**
     * Goes to the first visible char.
     * @param doc
     * @param cursorOffset
     */
    protected void gotoFirstVisibleChar(IDocument doc, int cursorOffset) {
        try {
            setCaretPosition(TextSelectionUtils.getFirstCharPosition(doc, cursorOffset));
        } catch (BadLocationException e) {
            beep(e);
        }
    }

    /**
     * Goes to the first visible char.
     * @param doc
     * @param cursorOffset
     */
    protected boolean isAtFirstVisibleChar(IDocument doc, int cursorOffset) {
        try {
            return TextSelectionUtils.getFirstCharPosition(doc, cursorOffset) == cursorOffset;
        } catch (BadLocationException e) {
            return false;
        }
    }

    @Override
    protected void setCaretPosition(int pos) throws BadLocationException {
        viewer.setSelectedRange(pos, 0);
    }

    /**
     * Creates a handler that will properly treat the line start command.
     */
    public static VerifyKeyListener createVerifyKeyListener(final SourceViewer viewer) {
        return new VerifyKeyListener() {

            @Override
            public void verifyKey(VerifyEvent event) {
                if (event.doit) {
                    boolean isHome;
                    isHome = KeyBindingHelper.matchesCommandKeybinding(event,
                            ITextEditorActionDefinitionIds.LINE_START);
                    if (isHome) {
                        ISelection selection = viewer.getSelection();
                        if (selection instanceof ITextSelection) {
                            FirstCharAction firstCharAction = new FirstCharAction();
                            firstCharAction.viewer = viewer;
                            firstCharAction.perform(viewer.getDocument(), (ITextSelection) selection);
                            event.doit = false;
                        }
                    }
                }
            }
        };
    }

}