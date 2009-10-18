/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.refactoring.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;

/**
 * @author Fabio Zadrozny
 */
public class PyRename extends PyRefactorAction {

    /**
     * we need:
     * 
     *     renameByCoordinates(filename, line, column, newname)
     */
    protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
        String res = "";
        res = AbstractPyRefactoring.getPyRefactoring().rename(getRefactoringRequest(monitor));
        return res;
    }


}
