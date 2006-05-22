/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ListResourceBundle;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;


public class ActionCreatorPyEditListener implements IPyEditListener{

    public void onSave(PyEdit edit) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
        edit.addOfflineActionListener("r", new PyRenameInFileAction(edit), "Rename occurrences in file", false);
    }

    public void onDispose(PyEdit edit) {
    }

    public void onSetDocument(IDocument document, PyEdit edit) {
    }

}
