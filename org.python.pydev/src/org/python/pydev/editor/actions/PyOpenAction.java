/*
 * Author: atotic
 * Created on Apr 12, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.REF;

/**
 * Opens an editor and selects text in it.
 * 
 * Inspired by org.eclipse.jdt.ui.actions.OpenAction, but simplifies
 * all handling in a single class.
 */
public class PyOpenAction extends Action {
		
	public IEditorPart editor;


    public PyOpenAction() {
	}
		
	private void showInEditor(
		ITextEditor textEdit,
		Location start,
		Location end) {
		try {
			IDocument doc =
				textEdit.getDocumentProvider().getDocument(
					textEdit.getEditorInput());
			int s;
			s = start.toOffset(doc);
			int e = end == null ? s : end.toOffset(doc);
			TextSelection sel = new TextSelection(s, e - s);
			textEdit.getSelectionProvider().setSelection(sel);
		} catch (BadLocationException e1) {
			PydevPlugin.log(IStatus.ERROR, "Error setting selection", e1);
		}
	}
	

	public void run(ItemPointer p) {
		editor = null;
		Object file = p.file;
		
        if (file instanceof IFile)
			editor = PydevPlugin.doOpenEditor(((IFile)file).getFullPath(), true);
		else if (file instanceof IPath) {
			editor = PydevPlugin.doOpenEditor((IPath)file, true);
		}
		else if (file instanceof File) {
			Path path = new Path(REF.getFileAbsolutePath((File)file));
			editor = PydevPlugin.doOpenEditor(path, true);
		}
		if (editor instanceof ITextEditor && p.start.line >= 0) {
			showInEditor((ITextEditor)editor, p.start, p.end);
		}
	}
}
