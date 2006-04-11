package com.python.pydev.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;

import com.python.pydev.ui.hierarchy.PyHierarchyView;

/**
 * 
 * Based on 
 * org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction
 * org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil
 * @author fabioz
 *
 */
public class PyShowHierarchy extends PyAction{

	public void run(IAction action) {
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			IWorkbenchPage page= workbenchWindow.getActivePage();
			PyHierarchyView view = (PyHierarchyView) page.showView("com.python.pydev.ui.hierarchy.PyHierarchyView");
			if(view != null){
				//set whatever is needed for the hierarchy
			}
			
		} catch (Exception e) {
			Log.log(e);
		}
	}

}
