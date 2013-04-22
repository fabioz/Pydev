package org.python.pydev.shared_ui;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.utils.Reflection;

public class EditorUtils {

    public static File getFileFromEditorInput(IEditorInput editorInput) {
        File f = null;
        IFile file = (IFile) editorInput.getAdapter(IFile.class);
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
        if (!(contributor instanceof EditorActionBarContributor))
            return null;

        IActionBars actionBars = ((EditorActionBarContributor) contributor).getActionBars();
        if (actionBars == null)
            return null;

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
}
