/*
 * Author: atotic
 * Created on Apr 12, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.plugin.ExternalEditorInput;
import org.python.pydev.plugin.FileStorage;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Opens an editor and selects text in it.
 * 
 * Inspired by {@link org.eclipse.jdt.ui.actions.OpenAction}, but simplifies
 * traversal of functions.
 */
public class PyOpenAction extends Action {
		
	public PyOpenAction() {
	}
	
	/**
	 * Opens the file, and sets the selection.
	 * @param start: ok if null, won't set selection
	 * @param end
	 */
	private void openInFile(IFile file, Location start, Location end) {
		if (file == null)
			return;
		IWorkbenchPage p = PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();	
		try {
			IEditorPart editorPart= p.openEditor(file, null, true);
			if (start != null &&
				editorPart instanceof ITextEditor) {
					ITextEditor textEdit = (ITextEditor)editorPart;
					showInEditor(textEdit, start, end);
				}
		} catch (PartInitException e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error opening file", e);
		} catch (BadLocationException e1) {
			PydevPlugin.log(IStatus.ERROR, "Error setting selection", e1);
		}
	}

	private void openInExternalFile(IPath path, Location start, Location end) {
		IStorage storage = new FileStorage(path);
		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor desc = registry.getDefaultEditor(path.lastSegment());
		if (desc == null)
			desc = registry.getDefaultEditor();
		IEditorInput input = new ExternalEditorInput(storage);
		IWorkbenchPage p = PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();	
		try {
			IEditorPart editor = p.openEditor(input, desc.getId());
		} catch (PartInitException e) {
			PydevPlugin.log(IStatus.ERROR, "Unexpected error opening external file", e);
		}	
	}
	
	private void showInEditor(ITextEditor textEdit, Location start, Location end)
		throws BadLocationException {
		IDocument doc = textEdit.getDocumentProvider().getDocument(textEdit.getEditorInput());
		int s;
			s = start.toOffset(doc);
		int e = end == null ? s : end.toOffset(doc);
		TextSelection sel = new TextSelection(s, e - s);
		textEdit.getSelectionProvider().setSelection(sel);
	}
	

	
	public void run(ItemPointer p) {
		if (p.file instanceof IFile)
			openInFile((IFile)p.file, p.start, p.end);
		else if (p.file instanceof File) {
			IPath path = new Path(((File)p.file).getAbsolutePath());
			IWorkspace w = ResourcesPlugin.getWorkspace();
			IFile file = w.getRoot().getFileForLocation(path);
			if (file != null && file.exists())
				openInFile(file, p.start, p.end);
			else {
				openInExternalFile(path, p.start, p.end);
			}
//			I wrote a plugin that does something like this; see the Fileopen plugin at
//			www.eclipsepowered.org (open source on sourceforge). Since you don't want to
//			save the file it's even easier, see the sample code posted for
//			https://bugs.eclipse.org/bugs/show_bug.cgi?id=2869 . Basically it's the same
//			idea used by the CVS plugin to view source code on an arbitrary revision, so
//			that's another place to look. HTH.
		}
		else {
			PydevPlugin.log(IStatus.ERROR, "Unknown ItemPointer file format", null);
		}
	}
}
