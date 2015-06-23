/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.python.pydev.core.log.Log;
import org.python.pydev.editorinput.EditorInputFactory;
import org.python.pydev.navigator.PythonpathTreeNode;
import org.python.pydev.navigator.PythonpathZipChildTreeNode;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileStorage;

/**
 * This open action extends the action that tries to open files with the Pydev Editor, just changing the implementation
 * to try to open the files with the 'correct' editor in the ide.
 */
public class PyOpenExternalAction extends PyOpenPythonFileAction {

    private IWorkbenchPage page;

    public PyOpenExternalAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        super(page, selectionProvider);
        this.page = page;
        this.setText("Open with System Editor");
    }

    @Override
    protected void openFiles(PythonpathTreeNode[] pythonPathFilesSelected) {
        for (PythonpathTreeNode n : pythonPathFilesSelected) {
            try {
                IDE.openEditor(page, EditorInputFactory.create(n.file, false),
                        IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
            } catch (PartInitException e) {
                Log.log(e);
            }
        }
    }

    @Override
    protected void openFiles(PythonpathZipChildTreeNode[] pythonPathFilesSelected) {
        for (PythonpathZipChildTreeNode n : pythonPathFilesSelected) {
            try {
                PydevZipFileStorage storage = new PydevZipFileStorage(n.zipStructure.file, n.zipPath);
                PydevZipFileEditorInput input = new PydevZipFileEditorInput(storage);
                IDE.openEditor(page, input, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
            } catch (PartInitException e) {
                Log.log(e);
            }
        }
    }

    /**
     * Overridden to open the given files with the match provided by the platform.
     */
    @Override
    protected void openFiles(List<IFile> filesSelected) {
        for (IFile f : filesSelected) {
            try {
                IDE.openEditor(page, f, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
            } catch (PartInitException e) {
                Log.log(e);
            }
        }
    }

    /**
     * @return whether the current selection enables this action (not considering selected containers).
     */
    @Override
    public boolean isEnabledForSelectionWithoutContainers() {
        fillSelections();

        //only available for the files we generate (the default is already available in other cases)
        //note it's not available for .zip resources
        if (pythonPathFilesSelected.size() > 0) {
            return true;
        }
        return false;
    }

}
