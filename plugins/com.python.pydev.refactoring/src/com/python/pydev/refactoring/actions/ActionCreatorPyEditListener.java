/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;


public class ActionCreatorPyEditListener implements IPyEditListener{

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("r", new PyRenameInFileAction(edit), "Rename occurrences in file", false);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
