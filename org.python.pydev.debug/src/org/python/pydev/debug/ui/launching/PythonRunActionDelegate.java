/*
 * Author: atotic
 * Created: Aug 27, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;

/**
 * Implements "Run Python..." extension for org.eclipse.ui.popupMenus.
 * 
 * <p>Passes off the selected file to {@link org.python.pydev.debug.ui.launching.LaunchShortcut LaunchShortcut}.
 * 
 * @see org.python.pydev.debug.ui.launching.LaunchShortcut 
 */
public class PythonRunActionDelegate extends ActionDelegate
	implements IObjectActionDelegate {

	private IFile selectedFile;
	private IWorkbenchPart part;

	public void run(IAction action) {
		if (part != null && selectedFile != null) {
			// figure out run or debug mode
			String runMode = action.getId().endsWith("RunPythonAction") ? 
												ILaunchManager.RUN_MODE : ILaunchManager.DEBUG_MODE;
			LaunchShortcut shortcut = new LaunchShortcut();
			shortcut.setShowDialog(true);
			shortcut.launch(selectedFile, runMode, null);
		}
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFile = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedResource = structuredSelection.getFirstElement();
				if (selectedResource instanceof IFile)
					selectedFile = (IFile) selectedResource;
			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

}
