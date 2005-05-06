/*
 * Created on May 21, 2004
 *
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;

/**
 * @author Fabio Zadrozny
 *  
 */
public class PyGoToDefinition extends PyRefactorAction {

    protected boolean areRefactorPreconditionsOK(PyEdit edit) {

        if (edit.isDirty())
            edit.doSave(null);

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
            areRefactorPreconditionsOK(pyEdit);

            PyOpenAction openAction = (PyOpenAction) pyEdit.getAction(PyEdit.ACTION_OPEN);

            ItemPointer[] where = findDefinition(pyEdit);

            if (where == null) {
                return;
            }

            if (where.length > 0)
                openAction.run(where[0]);
            else
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param node
     * @return
     */
    private ItemPointer[] findDefinition(PyEdit pyEdit) {
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        return pyRefactoring.findDefinition(pyEdit, getStartLine(), getStartCol(), null);
    }

    protected String perform(IAction action, String name, Operation operation) throws Exception {
        return null;
    }

    protected String getInputMessage() {
        return null;
    }

}