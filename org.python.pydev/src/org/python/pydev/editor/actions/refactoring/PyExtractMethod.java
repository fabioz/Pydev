/*
 * Created on Sep 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.refactoring.PyRefactoring;

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
    protected void perform(IAction action) throws BadLocationException, CoreException {
        File editorFile = getPyEdit().getEditorFile();
        
        //testing first with whole lines.
        int beginLine = getStartLine();
        int beginCol  = getStartCol();

        int endLine   = getEndLine();
        int endCol    = getEndCol();
        
        String name = getInput(getPyEdit(),"Please inform the new name.");

        if(name.equals("") == false){
	        PyRefactoring.getPyRefactoring().extract(editorFile, beginLine, beginCol, endLine, endCol, name);

	        refreshEditor(getPyEdit());
        }

    }
    
    
    

}
