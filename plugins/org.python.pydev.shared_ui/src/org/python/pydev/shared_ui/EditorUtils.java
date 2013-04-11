package org.python.pydev.shared_ui;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.shared_core.log.Log;
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
}
