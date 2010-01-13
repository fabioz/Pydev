/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package com.python.pydev.analysis.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class PyGlobalsBrowserWorkbench implements IWorkbenchWindowActionDelegate {

	private ISelection selection;
	
    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }

    public void run(IAction action) {
    	String text = null;
    	if(this.selection instanceof ITextSelection){
			ITextSelection textSelection = (ITextSelection) this.selection;
			text = textSelection.getText();
    	}
    	
        PyGlobalsBrowser.getFromWorkspace(text);
    }

    public void selectionChanged(IAction action, ISelection selection) {
    	this.selection = selection;
    }

}
