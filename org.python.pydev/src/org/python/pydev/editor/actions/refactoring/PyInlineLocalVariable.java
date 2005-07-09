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
        
        //testing first with whole lines.
        int beginLine = getStartLine();
        int beginCol  = getStartCol();

        return AbstractPyRefactoring.getPyRefactoring().inlineLocalVariable(getPyEdit(), beginLine, beginCol, operation);

    }
    
    protected String getInputMessage() {
        return null;
    }

}
