/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.refactoring.PyRefactoring;

/**
 * @author Fabio Zadrozny
 */
public class PyRename extends PyRefactorAction {

    /**
     * we need:
     * 
     *     renameByCoordinates(filename, line, column, newname)
     */
    protected void perform(IAction action) throws Exception {
        File editorFile = getPyEdit().getEditorFile();
        
        //testing first with whole lines.
        int beginLine = getStartLine();
        int beginCol  = getStartCol();

        String name = getInput(getPyEdit(),"Please inform the new name.");

        if(name.equals("") == false){
	        PyRefactoring.getPyRefactoring().rename(editorFile, beginLine, beginCol, name);

	        refreshEditor(getPyEdit());
        }
        
    }


}
