/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.MisconfigurationException;
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
     * @throws MisconfigurationException 
     */
    protected String perform(IAction action, String name, IProgressMonitor monitor) throws BadLocationException, CoreException, MisconfigurationException {
        return getPyRefactoring().inlineLocalVariable(getRefactoringRequest(monitor));
    }
    
    IPyRefactoring pyRefactoring;
    
    /**
     * @return the IPyRefactoring engine to be used
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
