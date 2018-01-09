package org.python.pydev.editor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.PySelection;

public class PySelectionFromEditor {

    /**
     * Alternate constructor for PySelection. Takes in a text editor from Eclipse.
     *
     * @param textEditor The text editor operating in Eclipse
     */
    public static PySelection createPySelectionFromEditor(ITextEditor textEditor) {
        return new PySelection(textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()),
                (ITextSelection) textEditor
                        .getSelectionProvider().getSelection());
    }
}
