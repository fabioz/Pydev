/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;

/**
 * @author Fabio Zadrozny
 */
public class PyExtractMethod extends PyRefactorAction {


    /**
     * we need:
     * 
	 *  def extract(self, filename_path, 
	 *              begin_line, begin_col,
	 *              end_line, end_col, 
	 *              name):
     * @throws BadLocationException
     * @throws CoreException
     */
    protected String perform(IAction action, String name, Operation operation) throws BadLocationException, CoreException {
        
        
        String res = "";
        if(name.equals("") == false){
	        res = getPyRefactoring("canExtract").extract(getRefactoringRequest(name, operation));
        }
        return res;

    }
    
    protected String getInputMessage() {
        return "Please inform the new name.";
    }

    

}
