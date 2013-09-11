/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Opens an editor and selects text in it.
 * 
 * Inspired by org.eclipse.jdt.ui.actions.OpenAction, but simplifies all handling in a single class.
 */
public class PyOpenAction extends Action {

    public IEditorPart editor;

    public PyOpenAction() {
    }

    public static void showInEditor(ITextEditor textEdit, Location start, Location end) {
        EditorUtils.showInEditor(textEdit, start, end);
    }

    public void run(ItemPointer p, IProject project) {
        editor = null;
        Object file = p.file;
        String zipFilePath = p.zipFilePath;

        if (zipFilePath != null) {
            //currently, only open zip file 
            editor = PyOpenEditor.doOpenEditor((File) file, zipFilePath);

        } else if (file instanceof IFile) {
            IFile f = (IFile) file;
            editor = PyOpenEditor.doOpenEditor(f);

        } else if (file instanceof IPath) {
            IPath path = (IPath) file;
            editor = PyOpenEditor.doOpenEditor(path, project);

        } else if (file instanceof File) {
            String absPath = FileUtils.getFileAbsolutePath((File) file);
            IPath path = Path.fromOSString(absPath);
            editor = PyOpenEditor.doOpenEditor(path, project);
        }

        if (editor instanceof ITextEditor && p.start.line >= 0) {
            EditorUtils.showInEditor((ITextEditor) editor, p.start, p.end);
        }
    }

    public void run(ItemPointer p) {
        run(p, null);
    }
}
