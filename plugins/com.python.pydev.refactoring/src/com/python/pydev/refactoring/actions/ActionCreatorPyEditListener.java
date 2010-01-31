/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.refactoring.ui.findreplace.FindReplaceAction;


public class ActionCreatorPyEditListener implements IPyEditListener{

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("r", new PyRenameInFileAction(edit), "Rename occurrences in file", false);
        
        
		// -------------------------------------------------------------------------------------
		// Find/Replace 
		FindReplaceAction action = new FindReplaceAction(resources, "Editor.FindReplace.", edit);
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		action.setId("org.python.pydev.editor.actions.findAndReplace");
		edit.setAction(ITextEditorActionConstants.FIND, action);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
