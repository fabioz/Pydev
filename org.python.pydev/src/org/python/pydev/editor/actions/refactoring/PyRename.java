/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.jface.action.IAction;
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
    protected String perform(IAction action, String name, Operation operation) throws Exception {
        String res = "";
        if(name.equals("") == false){
	        res = getPyRefactoring("canRename").rename(getRefactoringRequest(name, operation));
        }
        return res;
    }

    protected String getInputMessage() {
        return "Please inform the new name.";
    }

    /**
     * @return
     */
    protected String getDefaultValue() {
        return ps.getTextSelection().getText();
    }

    

}
