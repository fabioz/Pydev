package org.python.pydev.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.CoreTextSelection;

public class PySelectionFromEditor {

    /**
     * Alternate constructor for PySelection. Takes in a text editor from Eclipse.
     *
     * @param textEditor The text editor operating in Eclipse
     */
    public static PySelection createPySelectionFromEditor(ITextEditor textEditor) {
        IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        ITextSelection selection = (ITextSelection) textEditor
                .getSelectionProvider().getSelection();
        return new PySelection(document, new CoreTextSelection(document, selection.getOffset(), selection.getLength()));
    }

    public static PySelection createPySelectionFromEditor(ISourceViewer viewer, ITextSelection textSelection) {
        return new PySelection(viewer.getDocument(), new CoreTextSelection(
                viewer.getDocument(), textSelection.getOffset(), textSelection.getLength()));
    }

    public static PySelection createPySelectionFromEditor(ITextViewer viewer, ITextSelection textSelection) {
        return new PySelection(viewer.getDocument(), new CoreTextSelection(
                viewer.getDocument(), textSelection.getOffset(), textSelection.getLength()));
    }
}
