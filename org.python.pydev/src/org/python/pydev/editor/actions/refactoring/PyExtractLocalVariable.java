/*
 * Created on Oct 19, 2004
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
public class PyExtractLocalVariable extends PyRefactorAction {

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
        //testing first with whole lines.
        int beginLine = getStartLine();
        int beginCol  = getStartCol();

        int endLine   = getEndLine();
        int endCol    = getEndCol();
        
        String res = "";
        if(name.equals("") == false){
            res = AbstractPyRefactoring.getPyRefactoring().extractLocalVariable(getPyEdit(), beginLine, beginCol, endLine, endCol, name, operation);
        }
        return res;

    }
    
    protected String getInputMessage() {
        return "Please inform the new name.";
    }

}
