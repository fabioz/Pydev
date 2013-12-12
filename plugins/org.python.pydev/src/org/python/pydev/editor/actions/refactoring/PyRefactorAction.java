/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyRefactorAction extends PyAction {

    protected IWorkbenchWindow workbenchWindow;

    private final class Operation extends WorkspaceModifyOperation {

        /**
         * The action to be performed
         */
        private final IAction action;

        public Operation(IAction action) {
            super();
            this.action = action;
        }

        /**
         * Execute the actual refactoring (needs to ask for user input if not using the default
         * refactoring cycle)
         */
        @Override
        protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
                InterruptedException {

            try {
                monitor.beginTask("Refactor", IProgressMonitor.UNKNOWN);
                perform(action, monitor);
                monitor.done();
            } catch (Exception e) {
                Log.log(e);
            }

        }
    }

    public RefactoringRequest getRefactoringRequest() throws MisconfigurationException {
        return getRefactoringRequest(null);
    }

    /**
     * This is the refactoring request
     */
    protected volatile RefactoringRequest request;

    /**
     * @return the refactoring request (it is created and cached if still not available)
     * @throws MisconfigurationException 
     */
    public RefactoringRequest getRefactoringRequest(IProgressMonitor monitor) throws MisconfigurationException {
        if (request == null) {
            //testing first with whole lines.
            PyEdit pyEdit = getPyEdit(); //may not be available in tests, that's why it is important to be able to operate without it
            request = createRefactoringRequest(monitor, pyEdit, ps);
        }
        request.pushMonitor(monitor);
        return request;
    }

    /**
     * @param operation the operation we're doing (may be null)
     * @param pyEdit the editor from where we'll get the info
     * @throws MisconfigurationException 
     */
    public static RefactoringRequest createRefactoringRequest(IProgressMonitor monitor, PyEdit pyEdit, PySelection ps)
            throws MisconfigurationException {
        File file = pyEdit.getEditorFile();
        IPythonNature nature = pyEdit.getPythonNature();

        RefactoringRequest req = new RefactoringRequest(file, ps, monitor, nature, pyEdit);
        return req;

    }

    /**
     * Checks if the refactoring preconditions are met.
     * @param request the request for the refactoring
     * @param pyRefactoring the engine to do the refactoring
     * @return true if they are ok and false otherwise
     */
    protected boolean areRefactorPreconditionsOK(RefactoringRequest request, IPyRefactoring pyRefactoring) {
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
     * Actually executes this action.
     * 
     * Checks preconditions... if 
     */
    public void run(final IAction action) {
        // Select from text editor
        request = null; //clear the cache from previous runs
        ps = new PySelection(getTextEditor());

        RefactoringRequest req;
        try {
            req = getRefactoringRequest();
        } catch (MisconfigurationException e2) {
            Log.log(e2);
            return;
        }
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        if (areRefactorPreconditionsOK(req, pyRefactoring) == false) {
            return;
        }

        UIJob job = new UIJob("Performing: " + this.getClass().getName()) {

            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                try {
                    Operation o = new Operation(action);
                    o.execute(monitor);
                } catch (Exception e) {
                    Log.log(e);
                }
                return Status.OK_STATUS;
            }

        };
        job.setSystem(true);
        job.schedule();
    }

    /**
     * @return a shell from the PyEdit
     */
    protected Shell getPyEditShell() {
        return getPyEdit().getSite().getShell();
    }

    /**
     * This is the method that should be actually overridden to perform the refactoring action.
     * 
     * @param action the action to be performed 
     * @param monitor the monitor for the operation
     * @return the status returned by the server for the refactoring.
     * @throws Exception
     */
    protected abstract String perform(IAction action, IProgressMonitor monitor) throws Exception;

}