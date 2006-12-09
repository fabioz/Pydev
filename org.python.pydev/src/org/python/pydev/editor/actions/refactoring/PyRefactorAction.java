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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.IDocument;
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
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.uiutils.RunInUiThread;
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

        protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                InterruptedException {

            try {
                this.monitor = monitor;
                monitor.beginTask("Refactor", IProgressMonitor.UNKNOWN);
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
        InputDialog d = new InputDialog(getPyEditShell(), "Refactoring", msg, getDefaultValue(), null);

        int retCode = d.open();
        if (retCode == InputDialog.OK) {
            return d.getValue();
        }
        return "";
    }

    /**
     * @return
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
    public RefactoringRequest getRefactoringRequest(Operation operation){
    	return getRefactoringRequest(null, operation);
    }
    protected RefactoringRequest request; 
    public RefactoringRequest getRefactoringRequest(String name, Operation operation){
        if(request == null){
            //testing first with whole lines.
            PyEdit pyEdit = getPyEdit(); //may not be available in tests, that's why it is important to be able to operate without it
    		request = createRefactoringRequest(operation, pyEdit, ps);
        }
        request.operation = operation;
        request.duringProcessInfo.name = name;
		return request;
    }

    /**
     * @param operation the operation we're doing (may be null)
     * @param pyEdit the editor from where we'll get the info
     */
    public static RefactoringRequest createRefactoringRequest(Operation operation, PyEdit pyEdit, PySelection ps) {
        File file = pyEdit.getEditorFile();
        IDocument doc = pyEdit.getDocument();
        IPythonNature nature = pyEdit.getPythonNature();
        return new RefactoringRequest(file, doc, ps, operation, nature, pyEdit);
    }

    private void refreshEditor(PyEdit edit) throws CoreException {
        IFile file = (IFile) ((FileEditorInput) edit.getEditorInput()).getAdapter(IFile.class);
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

    protected PySelection ps;
    protected abstract IPyRefactoring getPyRefactoring();


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
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
        
        if(!pyRefactoring.useDefaultRefactoringActionCycle()){
            //this way, we don't provide anything to sync, ask the input, etc... that's all up to the 
            //pyrefactoring instance in the perform action
            new Job("Performing: "+action.getClass().getName()){

                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    RunInUiThread.sync(new Runnable(){

                        public void run() {
                            try{
                                Operation o = new Operation(null, action);
                                o.execute(monitor);
                            } catch (Exception e) {
                                PydevPlugin.log(e);
                            }
                        }
                        
                    });
                    return Status.OK_STATUS;
                }
                
            }.schedule();
            return;
        }

        String msg = getInputMessage();
        String name = "";
        if (msg != null)
            name = getInput(getPyEdit(), msg);

        final String nameUsed = name;
        Operation operation = new Operation(nameUsed, action);

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

        if (operation.statusOfOperation.startsWith("ERROR:")) {
            restartRefactorShell();
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
     *  
     */
    private void restartRefactorShell() {
        Thread thread = new Thread() {
            public void run() {
                AbstractPyRefactoring.restartShells();
            }
        };
        thread.setName("Restart Refactor Shell");
        thread.start();

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
    protected abstract String perform(IAction action, String name, Operation operation) throws Exception;

    /**
     * 
     * @return null if no input message is needed.
     */
    protected abstract String getInputMessage();

    /**
     * should throw an exception if we cannot do a refactoring in this editor.
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