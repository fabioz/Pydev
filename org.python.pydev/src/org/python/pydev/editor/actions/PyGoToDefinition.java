/*
 * Created on May 21, 2004
 *
 */
package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 *  
 */
public class PyGoToDefinition extends PyRefactorAction {

    protected boolean areRefactorPreconditionsOK(RefactoringRequest request) {
        try {
            checkAvailableForRefactoring(request);
        } catch (Exception e) {
        	e.printStackTrace();
            ErrorDialog.openError(null, "Error", "Unable to do requested action", 
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, e.getMessage(), null));
            return false;
        }

        if (request.pyEdit.isDirty())
        	request.pyEdit.doSave(null);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {

            ps = new PySelection(getTextEditor());
            PyEdit pyEdit = getPyEdit();
            areRefactorPreconditionsOK(getRefactoringRequest());

            PyOpenAction openAction = (PyOpenAction) pyEdit.getAction(PyEdit.ACTION_OPEN);

            ItemPointer[] where = findDefinition(pyEdit);

            if (where == null) {
                return;
            }

            if (where.length > 0){
                openAction.run(where[0]);
            } else {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
            }
        } catch (Exception e) {
        	e.printStackTrace();
            ErrorDialog.openError(null, "Error", "Unable to do requested action", 
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, e.getMessage(), null));
            
        }
    }

    /**
     * @param node
     * @return
     */
    private ItemPointer[] findDefinition(PyEdit pyEdit) {
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        return pyRefactoring.findDefinition(getRefactoringRequest());
    }

    protected String perform(IAction action, String name, Operation operation) throws Exception {
        return null;
    }

    protected String getInputMessage() {
        return null;
    }

}