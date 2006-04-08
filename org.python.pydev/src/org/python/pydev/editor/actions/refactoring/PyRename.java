/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.refactoring.IPyRefactoring;

/**
 * @author Fabio Zadrozny
 */
public class PyRename extends PyRefactorAction {

    /**
     * we need:
     * 
     *     renameByCoordinates(filename, line, column, newname)
     */
    protected String perform(IAction action, String name, Operation operation) throws Exception {
        String res = "";
        pyRefactoring = getPyRefactoring();
        res = pyRefactoring.rename(getRefactoringRequest(name, operation));
        return res;
    }

    IPyRefactoring pyRefactoring;
    /**
     * @return
     */
    protected IPyRefactoring getPyRefactoring() {
        if(pyRefactoring == null){
            pyRefactoring = getPyRefactoring("canRename"); 
        }
        return pyRefactoring;
    }

    protected String getInputMessage() {
        return "New value?";
    }

    /**
     * @return
     */
    protected String getDefaultValue() {
        return ps.getTextSelection().getText();
    }

    

}
