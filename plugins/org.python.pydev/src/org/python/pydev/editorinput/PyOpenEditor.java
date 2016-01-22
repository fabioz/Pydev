/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editorinput;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileStorage;

/**
 * Class that provides different ways to open an editor.
 *
 * @author fabioz
 */
public class PyOpenEditor {

    /**
     * Opens some editor from an editor input (See PySourceLocatorBase for obtaining it)
     *
     * @param file the editor input
     * @return the part correspondent to the editor
     * @throws PartInitException
     */
    public static IEditorPart openEditorInput(IEditorInput file) throws PartInitException {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            throw new RuntimeException("workbench cannot be null");
        }

        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            throw new RuntimeException(
                    "activeWorkbenchWindow cannot be null (we have to be in a ui thread for this to work)");
        }

        IWorkbenchPage wp = activeWorkbenchWindow.getActivePage();

        // File is inside the workspace
        return IDE.openEditor(wp, file, PyEdit.EDITOR_ID);
    }

    /**
     * Opens some editor from an IFile
     *
     * @see #openEditorInput(IEditorInput)
     */
    public static IEditorPart doOpenEditor(IFile f) {
        if (f == null) {
            return null;
        }

        try {
            FileEditorInput file = new FileEditorInput(f);
            return openEditorInput(file);

        } catch (Exception e) {
            Log.log(IStatus.ERROR, ("Unexpected error opening path " + f.toString()), e);
            return null;
        }
    }

    public static IEditorPart doOpenEditor(File file) {
        String absPath = FileUtils.getFileAbsolutePath(file);
        IPath path = Path.fromOSString(absPath);
        return PyOpenEditor.doOpenEditor(path, null);
    }

    /**
     * Utility function that opens an editor on a given path.
     *
     * @return part that is the editor
     * @see #openEditorInput(IEditorInput)
     */
    public static IEditorPart doOpenEditor(IPath path, IProject project) {
        if (path == null) {
            return null;
        }

        try {
            IEditorInput file = new PySourceLocatorBase().createEditorInput(path, project);
            if (file == null) {
                return null;
            }
            return openEditorInput(file);

        } catch (Exception e) {
            Log.log(IStatus.ERROR, ("Unexpected error opening path " + path.toString()), e);
            return null;
        }
    }

    /**
     * Utility function that opens an editor on a given path within a zip file.
     *
     * @return part that is the editor
     * @see #openEditorInput(IEditorInput)
     */
    public static IEditorPart doOpenEditor(File zipFile, String zipFilePath) {
        if (zipFile == null || zipFilePath == null) {
            return null;
        }

        try {
            IEditorInput file = new PydevZipFileEditorInput(new PydevZipFileStorage(zipFile, zipFilePath));
            return openEditorInput(file);

        } catch (Exception e) {
            Log.log(IStatus.ERROR,
                    ("Unexpected error opening zip file " + zipFile.getAbsolutePath() + " - " + zipFilePath), e);
            return null;
        }
    }

    /**
     * Open an editor anywhere on the file system using Eclipse's default editor registerd for the given file.
     *
     * @param fileToOpen File to open
     * @note we must be in the UI thread for this method to work.
     * @return Editor opened or created
     */
    public static IEditorPart doOpenEditorOnFileStore(File fileToOpen) {
        return EditorUtils.openFile(fileToOpen);
    }

}
