package org.python.pydev.debug.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.editor.PyEdit;

public class StepIntoEditorSelectionHandler extends AbstractHandler {

    IDebugEventSetListener listener = null;

    /* (non-Javadoc)
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (editor instanceof PyEdit) {
            PyEdit pyEdit = (PyEdit) editor;
            StepIntoSelectionHyperlinkDetector stepIntoSelectionHyperlinkDetector = new StepIntoSelectionHyperlinkDetector();
            ISelection selection = editor.getEditorSite().getSelectionProvider().getSelection();
            if (selection instanceof ITextSelection) {
                stepIntoSelectionHyperlinkDetector.setContext(pyEdit);
                ITextSelection iTextSelection = (ITextSelection) selection;
                IHyperlink[] detectHyperlinks = stepIntoSelectionHyperlinkDetector.detectHyperlinks(
                        pyEdit.getISourceViewer(),
                        new Region(iTextSelection.getOffset(), iTextSelection.getLength()), false);
                if (detectHyperlinks != null && detectHyperlinks.length > 0) {
                    detectHyperlinks[0].open();
                }
            }

        }
        return null;
    }
}