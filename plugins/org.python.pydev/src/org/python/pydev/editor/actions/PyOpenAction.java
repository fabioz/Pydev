/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 12, 2004
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
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editorinput.PyOpenEditor;

/**
 * Opens an editor and selects text in it.
 * 
 * Inspired by org.eclipse.jdt.ui.actions.OpenAction, but simplifies all handling in a single class.
 */
public class PyOpenAction extends Action {

    public IEditorPart editor;

    public PyOpenAction() {
    }

    public void showInEditor(ITextEditor textEdit, Location start, Location end) {
        try {
            IDocument doc = textEdit.getDocumentProvider().getDocument(textEdit.getEditorInput());
            int s = start.toOffset(doc);
            int e = end == null ? s : end.toOffset(doc);
            TextSelection sel = new TextSelection(s, e - s);
            textEdit.getSelectionProvider().setSelection(sel);
        } catch (BadLocationException e1) {
            if(textEdit instanceof PyEdit){
                PyEdit p = (PyEdit) textEdit;
                Log.log(IStatus.ERROR, ("Error setting selection:"+start+" - "+end+" - "+p.getEditorFile()), e1);
                
            }else{
                Log.log(IStatus.ERROR, ("Error setting selection:"+start+" - "+end), e1);
            }
        }
    }

    public void run(ItemPointer p) {
        editor = null;
        Object file = p.file;
        String zipFilePath = p.zipFilePath;
        
        if(zipFilePath != null){
            //currently, only open zip file 
            editor = PyOpenEditor.doOpenEditor((File)file, zipFilePath);
            
        } else if (file instanceof IFile) {
            IFile f = (IFile) file;
            editor = PyOpenEditor.doOpenEditor(f);

        } else if (file instanceof IPath) {
            IPath path = (IPath) file;
            editor = PyOpenEditor.doOpenEditor(path);

        } else if (file instanceof File) {
            String absPath = REF.getFileAbsolutePath((File) file);
            IPath path = Path.fromOSString(absPath);
            editor = PyOpenEditor.doOpenEditor(path);
        }

        if (editor instanceof ITextEditor && p.start.line >= 0) {
            showInEditor((ITextEditor) editor, p.start, p.end);
        }
    }
}
