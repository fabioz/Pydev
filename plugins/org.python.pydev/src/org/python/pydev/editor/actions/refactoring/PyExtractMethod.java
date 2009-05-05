/*
 * Created on Sep 14, 2004
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
     * @throws MisconfigurationException 
     */
    protected String perform(IAction action, String name, IProgressMonitor monitor) throws BadLocationException, CoreException, MisconfigurationException {
        
        
        String res = "";
        if(name.equals("") == false){
            res = getPyRefactoring().extract(getRefactoringRequest(name, monitor));
        }
        return res;

    }
    
    IPyRefactoring pyRefactoring;
    /**
     * @return
     */
    protected IPyRefactoring getPyRefactoring() {
        if(pyRefactoring == null){
            pyRefactoring = getPyRefactoring("canExtract"); 
        }
        return pyRefactoring;
    }

    
    protected String getInputMessage() {
        return "Please inform the new name.";
    }

    

}
