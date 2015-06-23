/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.utils.Reflection;
import org.python.pydev.shared_ui.editor_input.EditorInputUtils;

public class EditorUtils {

    public static File getFileFromEditorInput(IEditorInput editorInput) {
        File f = null;
        IFile file = editorInput.getAdapter(IFile.class);
        if (file != null) {
            IPath location = file.getLocation();
            if (location != null) {
                IPath path = location.makeAbsolute();
                f = path.toFile();
            }

        } else {
            try {
                if (editorInput instanceof IURIEditorInput) {
                    IURIEditorInput iuriEditorInput = (IURIEditorInput) editorInput;
                    return new File(iuriEditorInput.getURI());
                }
            } catch (Throwable e) {
                //OK, IURIEditorInput was only added on eclipse 3.3
            }

            try {
                IPath path = (IPath) Reflection.invoke(editorInput, "getPath", new Object[0]);
                f = path.toFile();
            } catch (Throwable e) {
                //ok, it has no getPath
            }
        }
        return f;
    }

    public static Shell getShell() {
        IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            Log.log("Error. Not currently with thread access (so, there is no activeWorkbenchWindow available)");
            return null;
        }
        return activeWorkbenchWindow.getShell();
    }

    /**
     * @return the active workbench window or null if it's not available.
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            return null;
        }
        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        return activeWorkbenchWindow;
    }

    /**
     * @return the active text editor or null if it's not available.
     */
    public static ITextEditor getActiveEditor() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) {
            IWorkbenchWindow activeWorkbenchWindow = workbench
                    .getActiveWorkbenchWindow();
            if (activeWorkbenchWindow != null) {
                IWorkbenchPage activePage = activeWorkbenchWindow
                        .getActivePage();
                if (activePage != null) {
                    IEditorPart activeEditor = activePage.getActiveEditor();
                    if (activeEditor instanceof ITextEditor) {
                        return (ITextEditor) activeEditor;
                    }
                }
            }
        }
        return null;
    }

    public static IStatusLineManager getStatusLineManager(ITextEditor editor) {
        IEditorActionBarContributor contributor = editor.getEditorSite().getActionBarContributor();
        if (!(contributor instanceof EditorActionBarContributor)) {
            return null;
        }

        IActionBars actionBars = ((EditorActionBarContributor) contributor).getActionBars();
        if (actionBars == null) {
            return null;
        }

        return actionBars.getStatusLineManager();
    }

    public static ITextSelection getTextSelection(ITextEditor editor) {
        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (!(selection instanceof ITextSelection)) {
            return null;
        }
        return (ITextSelection) selection;
    }

    public static IDocument getDocument(ITextEditor editor) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        if (documentProvider != null) {
            return documentProvider.getDocument(editor.getEditorInput());
        }
        return null;
    }

    public static TextSelectionUtils createTextSelectionUtils(ITextEditor editor) {
        return new TextSelectionUtils(getDocument(editor), getTextSelection(editor));
    }

    public static void showInEditor(ITextEditor textEdit, Location start, Location end) {
        try {
            IDocument doc = textEdit.getDocumentProvider().getDocument(textEdit.getEditorInput());
            int s = start.toOffset(doc);
            int e = end == null ? s : end.toOffset(doc);
            TextSelection sel = new TextSelection(s, e - s);
            textEdit.getSelectionProvider().setSelection(sel);
        } catch (BadLocationException e1) {
            Log.log(IStatus.ERROR, ("Error setting selection:" + start + " - " + end + " - " + textEdit), e1);
        }
    }

    public static void showInEditor(ITextEditor textEdit, IRegion region) {
        TextSelection sel = new TextSelection(region.getOffset(), region.getLength());
        textEdit.getSelectionProvider().setSelection(sel);
    }

    /**
     * Select the line given by lineNumber. Takes no effect if lineNumber <= 0 || lineNumber > number of lines in document
     * @param textEdit Text editor to select line in
     * @param lineNumber Line number to select. (First line in the editor is line 1)
     */
    public static void showInEditor(ITextEditor textEdit, int lineNumber) {
        // Setting line number programatically courtesy of
        // http://stackoverflow.com/questions/2873879/eclipe-pde-jump-to-line-x-and-highlight-it
        if (lineNumber > 0) {
            IDocument document = textEdit.getDocumentProvider().getDocument(
                    textEdit.getEditorInput());
            if (document != null) {
                IRegion lineInfo = null;
                try {
                    // line count internally starts with 0
                    lineInfo = document.getLineInformation(lineNumber - 1);
                } catch (BadLocationException e) {
                    // ignored because line number may not really exist in document,
                }
                if (lineInfo != null) {
                    textEdit.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
                }
            }
        }
    }

    public static IEditorPart openFile(File fileToOpen) {
        return openFile(fileToOpen, true);
    }

    /**
     * Open an editor anywhere on the file system using Eclipse's default editor registered for the given file.
     *
     * @param fileToOpen File to open
     * @note we must be in the UI thread for this method to work.
     * @return Editor opened or created
     */
    public static IEditorPart openFile(File fileToOpen, boolean activate) {

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

        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());

        try {
            if (activate) {
                // open the editor on the file
                return IDE.openEditorOnFileStore(wp, fileStore);
            }

            // Workaround when we don't want to activate (as there's no suitable API
            // in the core for that).
            IEditorInput input = getEditorInput(fileStore);
            String editorId = getEditorId(input, null);

            return wp.openEditor(input, editorId, activate);

        } catch (Exception e) {
            Log.log("Editor failed to open", e);
            return null;
        }
    }

    private static IEditorInput getEditorInput(IFileStore fileStore) {
        IFile workspaceFile = getWorkspaceFile(fileStore);
        if (workspaceFile != null) {
            return new FileEditorInput(workspaceFile);
        }
        return new FileStoreEditorInput(fileStore);
    }

    private static IFile getWorkspaceFile(IFileStore fileStore) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = root.findFilesForLocationURI(fileStore.toURI());
        files = filterNonExistentFiles(files);
        if (files == null || files.length == 0) {
            return null;
        }

        // for now only return the first file
        return files[0];
    }

    private static IFile[] filterNonExistentFiles(IFile[] files) {
        if (files == null) {
            return null;
        }

        int length = files.length;
        ArrayList existentFiles = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            if (files[i].exists()) {
                existentFiles.add(files[i]);
            }
        }
        return (IFile[]) existentFiles.toArray(new IFile[existentFiles.size()]);
    }

    /**
     * Open an editor anywhere on the file system using Eclipse's default editor registered for the given file.
     *
     * @param fileToOpen File to open
     * @note we must be in the UI thread for this method to work.
     * @return Editor opened or created
     */
    public static IEditorPart openFile(IFile fileToOpen) {
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
        try {
            return IDE.openEditor(wp, fileToOpen);
        } catch (Exception e) {
            Log.log("Editor failed to open", e);
            return null;
        }
    }

    public static IWorkbenchPartSite getSite() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return null;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return null;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor == null) {
            return null;
        }
        return activeEditor.getSite();
    }

    public static String getEditorId(IEditorInput input, Object element) {
        String name = input.getName();
        if (name == null) {
            return null;
        }

        try {
            IFile iFile = input.getAdapter(IFile.class);
            if (iFile != null) {
                IEditorDescriptor defaultEditor = IDE.getDefaultEditor(iFile);
                if (defaultEditor != null) {
                    return defaultEditor.getId();
                }
            }
        } catch (Exception e) {
            Log.log(e); // This shouldn't happen, but just in case...
        }

        IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();

        IEditorDescriptor defaultEditor = editorReg.getDefaultEditor(name);
        if (defaultEditor == null) {
            return null;
        }
        return defaultEditor.getId();
    }

    /**
     * @param statusLineManager optional (to set error messages).
     *
     * Return may have IFile and File objects.
     */
    public static List<Object> getFilesInOpenEditors(IStatusLineManager statusLineManager) {
        IWorkbenchWindow window = EditorUtils.getActiveWorkbenchWindow();
        if (window == null) {
            if (statusLineManager != null) {
                statusLineManager.setErrorMessage("Active workbench window is null.");
            }
            return new ArrayList<>(0);
        }
        IWorkbenchPage activePage = window.getActivePage();
        if (activePage == null) {
            if (statusLineManager != null) {
                statusLineManager.setErrorMessage("Active page is null.");
            }
            return new ArrayList<>(0);
        }
        IEditorReference editorsArray[] = activePage.getEditorReferences();

        final List<Object> files = new ArrayList<Object>();
        for (int i = 0; i < editorsArray.length; i++) {
            IEditorPart realEditor = editorsArray[i].getEditor(true);
            if (realEditor != null) {
                if (realEditor instanceof MultiPageEditorPart) {
                    try {
                        Method getPageCount = MultiPageEditorPart.class.getDeclaredMethod("getPageCount");
                        getPageCount.setAccessible(true);
                        Method getEditor = MultiPageEditorPart.class.getDeclaredMethod("getEditor", int.class);
                        getEditor.setAccessible(true);

                        Integer pageCount = (Integer) getPageCount.invoke(realEditor);
                        for (int j = 0; j < pageCount; j++) {
                            IEditorPart part = (IEditorPart) getEditor.invoke(realEditor, j);
                            if (part != null) {
                                IEditorInput input = part.getEditorInput();
                                if (input != null) {
                                    IFile file = input.getAdapter(IFile.class);
                                    if (file != null) {
                                        files.add(file);
                                    } else {
                                        File file2 = EditorInputUtils.getFile(input);
                                        if (file2 != null) {
                                            files.add(file2);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable e1) {
                        //Log it but keep going on.
                        Log.log(e1);
                    }

                } else {
                    IEditorInput input = realEditor.getEditorInput();
                    if (input != null) {
                        IFile file = input.getAdapter(IFile.class);
                        if (file != null) {
                            files.add(file);
                        } else {
                            File file2 = EditorInputUtils.getFile(input);
                            if (file2 != null) {
                                files.add(file2);
                            }
                        }
                    }
                }
            }
        }
        return files;
    }

}
