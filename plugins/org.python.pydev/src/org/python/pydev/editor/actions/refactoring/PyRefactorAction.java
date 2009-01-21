/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import java.io.File;
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
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.views.PyRefactorView;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyRefactorAction extends PyAction {

    protected IWorkbenchWindow workbenchWindow;

    private final class Operation extends WorkspaceModifyOperation {
        
        /**
         * A string with the status of the operation (only useful if using the default refactoring cycle.
         * Will be checked for a string starting with "ERROR:")
         */
        public String statusOfOperation;

        /**
         * The name the user chose (only available if using the default refactoring cycle)
         */
        private final String nameUsed;

        /**
         * The action to be performed
         */
        private final IAction action;

        public Operation(String nameUsed, IAction action) {
            super();
            this.nameUsed = nameUsed;
            this.action = action;
        }

        /**
         * Execute the actual refactoring (needs to ask for user input if not using the default
         * refactoring cycle)
         */
        protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                InterruptedException {

            try {
                monitor.beginTask("Refactor", IProgressMonitor.UNKNOWN);
                this.statusOfOperation = perform(action, nameUsed, monitor);
                monitor.done();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @param msg the message to be shown
     */
    protected String getInput(String msg) {
        InputDialog d = new InputDialog(getPyEditShell(), "Refactoring", msg, getDefaultValue(), null);

        int retCode = d.open();
        if (retCode == InputDialog.OK) {
            return d.getValue();
        }
        return "";
    }

    /**
     * @return the default value for some input dialog
     */
    protected String getDefaultValue() {
        return "";
    }
    
    /**
     * @param conditionalMethod this is the conditional method that must return true so that the 
     * object is used for what it is intended (like, checking if it can do a rename before asking it
     * to really do one.
     * @return the pyrefactoring to be used.
     */
    protected IPyRefactoring getPyRefactoring(String conditionalMethod){
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        //check if it is able to do the method by checking its pre-condition
        try {
            if ((Boolean) REF.invoke(pyRefactoring, conditionalMethod, new Object[0])) {
                return pyRefactoring;
            }
        } catch (Exception e) {
            //probably some plugin contributed something faulty...
            //just log it and ignore (keep with the default implementation, provided by brm)
            PydevPlugin.log(e);
        }
        return AbstractPyRefactoring.getDefaultPyRefactoring();
    }
    
    public RefactoringRequest getRefactoringRequest(){
        return getRefactoringRequest(null, null);
    }
    public RefactoringRequest getRefactoringRequest(IProgressMonitor monitor){
        return getRefactoringRequest(null, monitor);
    }
    
    /**
     * This is the refactoring request
     */
    protected volatile RefactoringRequest request; 
    
    /**
     * @return the refactoring request (it is created and cached if still not available)
     */
    public RefactoringRequest getRefactoringRequest(String name, IProgressMonitor monitor){
        if(request == null){
            //testing first with whole lines.
            PyEdit pyEdit = getPyEdit(); //may not be available in tests, that's why it is important to be able to operate without it
            request = createRefactoringRequest(monitor, pyEdit, ps);
        }
        request.pushMonitor(monitor);
        request.inputName = name;
        return request;
    }

    /**
     * @param operation the operation we're doing (may be null)
     * @param pyEdit the editor from where we'll get the info
     */
    public static RefactoringRequest createRefactoringRequest(IProgressMonitor monitor, PyEdit pyEdit, PySelection ps) {
        File file = pyEdit.getEditorFile();
        IPythonNature nature = pyEdit.getPythonNature();
        return new RefactoringRequest(file, ps, monitor, nature, pyEdit);
    }

    /**
     * Refreshes the given PyEdit
     */
    private void refreshEditor(PyEdit edit) throws CoreException {
        IFile file = (IFile) ((FileEditorInput) edit.getEditorInput()).getAdapter(IFile.class);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    /**
     * Refreshes the editors after a refactoring.
     * @throws CoreException
     */
    protected void refreshEditors(PyEdit edit) throws CoreException {
        refreshEditor(edit);

        IWorkbenchPage[] pages = workbenchWindow.getPages();
        for (int i = 0; i < pages.length; i++) {
            IEditorReference[] editorReferences = pages[i].getEditorReferences();

            IViewReference[] viewReferences = pages[i].getViewReferences();

            for (int j = 0; j < editorReferences.length; j++) {
                IEditorPart ed = editorReferences[j].getEditor(false);
                if (ed instanceof PyEdit) {
                    PyEdit e = (PyEdit) ed;
                    if (e != edit) {
                        try {
                            refreshEditor(e);
                        } catch (Exception e1) {
                        }
                    }
                }
            }

            for (int j = 0; j < viewReferences.length; j++) {
                IWorkbenchPart view = viewReferences[j].getPart(false);
                if (view instanceof PyRefactorView) {
                    view = viewReferences[j].getPart(true);
                    PyRefactorView e = (PyRefactorView) view;
                    e.refresh();
                }
            }

        }

    }

    /**
     * Checks if the refactoring preconditions are met.
     * @param request the request for the refactoring
     * @param pyRefactoring the engine to do the refactoring
     * @return true if they are ok and false otherwise
     */
    protected boolean areRefactorPreconditionsOK(RefactoringRequest request, IPyRefactoring pyRefactoring) {
        try {
            checkAvailableForRefactoring(request, pyRefactoring);
        } catch (Exception e) {
            ErrorDialog.openError(null, "Error", "Unable to do requested action", 
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, e.getMessage(), null));
            return false;
        }
        
        workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        IEditorPart[] dirtyEditors = workbenchWindow.getActivePage().getDirtyEditors();

        boolean saveEditors = false;
        if (dirtyEditors.length > 0) {
            saveEditors = MessageDialog.openQuestion(getPyEditShell(), "Save All?",
                    "All the editors must be saved to make this operation.\nIs it ok to save them?");
            if (saveEditors == false) {
                return false;
            }
        }

        if (saveEditors) {
            boolean editorsSaved = workbenchWindow.getActivePage().saveAllEditors(false);
            if (!editorsSaved) {
                return false;
            }
        }
        return true;
    }

    /**
     * This is the current text selection
     */
    protected PySelection ps;
    
    /**
     * @return the refactoring engine that should be used for this action (because one engine may provide
     * the extract method and the other the rename)
     */
    protected abstract IPyRefactoring getPyRefactoring();


    /**
     * Actually executes this action.
     * 
     * Checks preconditions... if 
     */
    public void run(final IAction action) {
        // Select from text editor
        request = null; //clear the cache from previous runs
        ps = new PySelection(getTextEditor());

        RefactoringRequest req = getRefactoringRequest();
        IPyRefactoring pyRefactoring = getPyRefactoring();
        if (areRefactorPreconditionsOK(req, pyRefactoring) == false) {
            return;
        }

        
        //Should we use the default refactoring cycle (basically, the BRM cycle) or not?
        if(!pyRefactoring.useDefaultRefactoringActionCycle()){
            //this way, we don't provide anything to sync, ask the input, etc... that's all up to the 
            //pyrefactoring instance in the perform action
            UIJob job = new UIJob("Performing: "+this.getClass().getName()){

                @Override
                public IStatus runInUIThread(final IProgressMonitor monitor) {
                    try{
                        Operation o = new Operation(null, action);
                        o.execute(monitor);
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                    return Status.OK_STATUS;
                }
                
            };
            job.setSystem(true);
            job.schedule();
            return;
        }

        //Now, if the user did choose the 'default' refactoring cycle, let's go on and ask the questions
        //needed and go on to the request
        String msg = getInputMessage();
        String name = "";
        if (msg != null){
            name = getInput(msg);
        }

        Operation operation = new Operation(name, action);

        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(getPyEditShell());
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

        if (operation.statusOfOperation != null && operation.statusOfOperation.startsWith("ERROR:")) {
            restartRefactorShell(req);
            String[] strings = operation.statusOfOperation.split("DETAILS:");

            if (strings.length == 2) {

                IStatus status = new Status(IStatus.ERROR, PydevPlugin.getPluginID(), 0, strings[0],
                        new Exception(strings[0]));

                ErrorDialog.openError(getPyEditShell(), "ERROR", strings[0], status);
            } else {
                MessageDialog.openError(getPyEditShell(), "ERROR", operation.statusOfOperation);
            }
            throw new RuntimeException(strings[1]);
        }

        // Put cursor at the first area of the selection
        getTextEditor().selectAndReveal(ps.getEndLine().getOffset(), 0);

    }

    /**
     * If the engine has a shell... restart it
     * @param req 
     */
    private void restartRefactorShell(final RefactoringRequest req) {
        Thread thread = new Thread() {
            public void run() {
                AbstractPyRefactoring.restartShells(req);
            }
        };
        thread.setName("Restart Refactor Shell");
        thread.start();

    }

    /**
     * @return a shell from the PyEdit
     */
    protected Shell getPyEditShell() {
        return getPyEdit().getSite().getShell();
    }

    /**
     * This is the method that should be actually overriden to perform the refactoring action.
     * 
     * @param action the action to be performed 
     * @param name the name that the user typed if using the default refactoring cycle (otherwise it is null)
     * @param monitor the monitor for the operation
     * @return the status returned by the server for the refactoring.
     * @throws Exception
     */
    protected abstract String perform(IAction action, String name, IProgressMonitor monitor) throws Exception;

    /**
     * @return null if no input message is needed.
     * @note only used in default refactoring cycle
     */
    protected abstract String getInputMessage();

    /**
     * Should throw an exception if we cannot do a refactoring in this editor.
     * @param pyRefactoring the refactoring engine
     */
    public static void checkAvailableForRefactoring(RefactoringRequest request, IPyRefactoring pyRefactoring) {
        IPythonNature pythonNature = request.nature;
        if(pythonNature == null){
            throw new RuntimeException("Unable to do refactor because the file is an a project that does not have the pydev nature configured.");
        }
        pyRefactoring.canRefactorNature(pythonNature);
    }
}