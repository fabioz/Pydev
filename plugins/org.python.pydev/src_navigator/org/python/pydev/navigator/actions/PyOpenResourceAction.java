/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.navigator.PythonpathTreeNode;
import org.python.pydev.navigator.PythonpathZipChildTreeNode;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileStorage;

/**
 * This open action extends the action that tries to open files with the Pydev Editor, just changing the implementation
 * to try to open the files with the 'correct' editor in the ide.
 */
public class PyOpenResourceAction extends PyOpenPythonFileAction {

    private IWorkbenchPage page;

    public PyOpenResourceAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        super(page, selectionProvider);
        this.page = page;
        this.setText("Open");
    }

    @Override
    protected void openFiles(PythonpathTreeNode[] pythonPathFilesSelected) {
        for (PythonpathTreeNode n : pythonPathFilesSelected) {
            try {
                if (PythonPathHelper.isValidSourceFile(n.file.getName())) {
                    new PyOpenAction().run(new ItemPointer(n.file));
                } else {
                    final IFileStore fileStore = EFS.getLocalFileSystem().getStore(n.file.toURI());
                    IDE.openEditorOnFileStore(page, fileStore);
                }
            } catch (PartInitException e) {
                Log.log(e);
            }
        }
    }

    @Override
    protected void openFiles(PythonpathZipChildTreeNode[] pythonPathFilesSelected) {
        for (PythonpathZipChildTreeNode n : pythonPathFilesSelected) {
            try {
                if (PythonPathHelper.isValidSourceFile(n.zipPath)) {
                    new PyOpenAction().run(new ItemPointer(n.zipStructure.file, new Location(), new Location(), null,
                            n.zipPath));
                } else {
                    IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
                    IEditorDescriptor defaultEditor = editorReg.getDefaultEditor(n.zipPath);
                    PydevZipFileStorage storage = new PydevZipFileStorage(n.zipStructure.file, n.zipPath);
                    PydevZipFileEditorInput input = new PydevZipFileEditorInput(storage);

                    if (defaultEditor != null) {
                        IDE.openEditor(page, input, defaultEditor.getId());
                    } else {
                        IDE.openEditor(page, input, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
                    }
                }
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
            PythonPathHelper.markAsPyDevFileIfDetected(f);
            try {
                IDE.openEditor(page, f);
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
        if (pythonPathFilesSelected.size() > 0 || pythonPathZipFilesSelected.size() > 0) {
            return true;
        }
        return false;
    }
}
