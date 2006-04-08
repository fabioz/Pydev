/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.refactoring.IPyRefactoring;

/**
 * @author Fabio Zadrozny
 * 
 */
public class PyInlineLocalVariable extends PyRefactorAction {
    
    /**
     * we need:
     * 
	 *  def inlineLocalVariable(self,filename_path, line, col)
	 * 
     * @throws BadLocationException
     * @throws CoreException
     */
    protected String perform(IAction action, String name, Operation operation) throws BadLocationException, CoreException {
        return getPyRefactoring().inlineLocalVariable(getRefactoringRequest(operation));
    }
    
    IPyRefactoring pyRefactoring;
    /**
     * @return
     */
    protected IPyRefactoring getPyRefactoring() {
        if(pyRefactoring == null){
            pyRefactoring = getPyRefactoring("canInlineLocalVariable"); 
        }
        return pyRefactoring;
    }

    
    protected String getInputMessage() {
        return null;
    }

}
