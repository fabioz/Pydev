/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.refactoring.PyRefactoring;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.views.PyRefactorView;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyRefactorAction extends PyAction {

    protected IWorkbenchWindow workbenchWindow;

    public final class Operation extends WorkspaceModifyOperation {
        public String statusOfOperation;

        private final String nameUsed;

        private final IAction action;

        public IProgressMonitor monitor;

        public Operation(String nameUsed, IAction action) {
            super();
            this.nameUsed = nameUsed;
            this.action = action;
        }

        protected void execute(IProgressMonitor monitor) throws CoreException,
                InvocationTargetException, InterruptedException {

            try {
                this.monitor = monitor;
                monitor.beginTask("Refactor", 500);
                this.statusOfOperation = perform(action, nameUsed, this);
                monitor.done();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @param edit
     * @param msg
     */
    protected String getInput(PyEdit edit, String msg) {
        InputDialog d = new InputDialog(getPyEditShell(), "Refactoring", msg,
                "", null);
        int retCode = d.open();
        if (retCode == InputDialog.OK) {
            return d.getValue();
        }
        return "";
    }

    private void refreshEditor(PyEdit edit) throws CoreException {
        IFile file = (IFile) ((FileEditorInput) edit.getEditorInput())
                .getAdapter(IFile.class);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    /**
     * @param edit
     * @throws CoreException
     */
    protected void refreshEditors(PyEdit edit) throws CoreException {
        refreshEditor(edit);
        


        IWorkbenchPage[] pages = workbenchWindow.getPages();
        for (int i = 0; i < pages.length; i++) {
            IEditorReference[] editorReferences = pages[i]
                    .getEditorReferences();

            IViewReference[] viewReferences = pages[i].getViewReferences();
            
            for (int j = 0; j < editorReferences.length; j++) {
                IEditorPart ed = editorReferences[j].getEditor(false);
                if (ed instanceof PyEdit) {
                    PyEdit e = (PyEdit) ed;
                    if (e != edit) {
                        refreshEditor(e);
                    }
                }
            }

        
            for (int j = 0; j < viewReferences.length; j++) {
                IWorkbenchPart view = viewReferences[j].getPart(false);
                if(view instanceof PyRefactorView){
                  view = viewReferences[j].getPart(true);
                  PyRefactorView e = (PyRefactorView) view;
                    e.refresh();
                }
            }

        }
        
    }

    /**
     * @param edit
     */
    protected boolean areRefactorPreconditionsOK(PyEdit edit) {
        workbenchWindow = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        IEditorPart[] dirtyEditors = workbenchWindow.getActivePage()
                .getDirtyEditors();

        boolean saveEditors = false;
        if (dirtyEditors.length > 0) {
            saveEditors = MessageDialog
                    .openQuestion(getPyEditShell(), "Save All?",
                            "All the editors must be saved to make this operation.\nIs it ok to save them?");
            if (saveEditors == false) {
                return false;
            }
        }

        if (saveEditors) {
            boolean editorsSaved = workbenchWindow.getActivePage()
                    .saveAllEditors(false);
            if (!editorsSaved) {
                return false;
            }
        }
        return true;
    }

    protected PySelection ps;

    /**
     * @return
     */
    protected int getEndCol() {
        return ps.absoluteCursorOffset + ps.selLength - ps.endLine.getOffset();
    }

    /**
     * @return
     */
    protected int getEndLine() {
        return ps.endLineIndex + 1;
    }

    /**
     * @return
     */
    protected int getStartCol() {
        return ps.absoluteCursorOffset - ps.startLine.getOffset();
    }

    /**
     * @return
     */
    protected int getStartLine() {
        return ps.startLineIndex + 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(final IAction action) {
        // Select from text editor
        ps = new PySelection(getTextEditor(), false);

        if (areRefactorPreconditionsOK(getPyEdit()) == false) {
            return;
        }

        String msg = getInputMessage();
        String name = "";
        if (msg != null)
            name = getInput(getPyEdit(), msg);

        final String nameUsed = name;
        Operation operation = new Operation(nameUsed, action);

        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(
                getPyEditShell());
        monitorDialog.setBlockOnOpen(false);
        try {
            monitorDialog.run(true, false, operation);
            // Perform the action
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            refreshEditors(getPyEdit());
        } catch (CoreException e1) {
            e1.printStackTrace();
        }

        if (operation.statusOfOperation.startsWith("ERROR:")) {
            restartRefactorShell();
            String[] strings = operation.statusOfOperation.split("DETAILS:");
            if (strings.length == 2) {
                IStatus status = new Status(IStatus.ERROR, PydevPlugin
                        .getPluginID(), 0, strings[0],
                        new Exception(strings[0]));
                ErrorDialog.openError(getPyEditShell(), "ERROR", strings[0],
                        status);
            } else {
                MessageDialog.openError(getPyEditShell(), "ERROR",
                        operation.statusOfOperation);
            }
            throw new RuntimeException(strings[1]);
        }

        // Put cursor at the first area of the selection
        getTextEditor().selectAndReveal(ps.endLine.getOffset(), 0);

    }

    /**
     *  
     */
    private void restartRefactorShell() {
        new Thread() {
            public void run() {
                PyRefactoring.getPyRefactoring().restartShell();
            }
        }.start();

    }

    /**
     * @return
     */
    protected Shell getPyEditShell() {
        return getPyEdit().getSite().getShell();
    }

    /**
     * 
     * @param action
     * @param name
     * @param operation
     * @return the status returned by the server for the refactoring.
     * @throws Exception
     */
    protected abstract String perform(IAction action, String name,
            Operation operation) throws Exception;

    /**
     * 
     * @return null if no input message is needed.
     */
    protected abstract String getInputMessage();
}