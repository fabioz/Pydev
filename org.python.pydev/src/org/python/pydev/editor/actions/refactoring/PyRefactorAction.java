/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyRefactorAction extends PyAction {

    /**
     * @param edit
     * @param msg
     */
    protected String getInput(PyEdit edit, String msg) {
        InputDialog d = new InputDialog(edit.getSite().getShell(),"Refactoring", msg,"",null);
        int retCode = d.open();
        if(retCode == InputDialog.OK){
            return d.getValue();
        }
        return "";
    }

    /**
     * @param edit
     * @throws CoreException
     */
    protected void refreshEditor(PyEdit edit) throws CoreException {
        IFile file = (IFile) ((FileEditorInput)edit.getEditorInput()).getAdapter(IFile.class);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    /**
     * @param edit
     */
    protected boolean areRefactorPreconditionsOK(PyEdit edit) {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorPart[] dirtyEditors = workbenchWindow.getActivePage().getDirtyEditors();
        
        boolean saveEditors = false;
        if(dirtyEditors.length > 0){
            saveEditors = MessageDialog.openQuestion(edit.getSite().getShell(), "Save All?", "All the editors must be saved to make this operation.\nIs it ok to save them?");
            if(saveEditors==false){
                return false;
            }
        } 
        
        if(saveEditors){
            boolean editorsSaved = workbenchWindow.getActivePage().saveAllEditors(false);
            if (!editorsSaved){
                return false;
            }
        }
        return true;
    }

    protected PySelection ps;

    /**
     * @return
     */
    protected int getEndCol() {
        return ps.absoluteCursorOffset + ps.selLength - ps.endLine.getOffset();
    }

    /**
     * @return
     */
    protected int getEndLine() {
        return ps.endLineIndex+1;
    }

    /**
     * @return
     */
    protected int getStartCol() {
        return ps.absoluteCursorOffset - ps.startLine.getOffset();
    }

    /**
     * @return
     */
    protected int getStartLine() {
        return ps.startLineIndex+1;
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        System.out.println("PyExtractMethod ");
		try 
		{
			// Select from text editor
			ps = new PySelection ( getTextEditor ( ), false );
	        
			if(areRefactorPreconditionsOK(getPyEdit())==false){
	            return;
	        }

	        // Perform the action
			perform ( action );

			// Put cursor at the first area of the selection
			getTextEditor ( ).selectAndReveal ( ps.endLine.getOffset ( ), 0 );
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}		

    }

    /**
     * @param action
     */
    protected abstract void perform(IAction action) throws Exception;

}
