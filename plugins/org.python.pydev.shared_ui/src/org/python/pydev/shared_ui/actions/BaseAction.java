package org.python.pydev.shared_ui.actions;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

public class BaseAction {

    /**
     * @return true if the contents of the editor may be changed. Clients MUST call this before actually
     * modifying the editor.
     */
    public static boolean canModifyEditor(ITextEditor editor) {

        if (editor instanceof ITextEditorExtension2) {
            return ((ITextEditorExtension2) editor).isEditorInputModifiable();

        } else if (editor instanceof ITextEditorExtension) {
            return !((ITextEditorExtension) editor).isEditorInputReadOnly();

        } else if (editor != null) {
            return editor.isEditable();

        }

        //If we don't have the editor, let's just say it's ok (working on document).
        return true;
    }
}
