package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.debug.model.PyPropertyTraceManager;
import org.python.pydev.debug.ui.PyPropertyTraceDialog;
import org.python.pydev.editor.actions.PyAction;

public class PyPropertyTraceAction extends PyAction implements
		IWorkbenchWindowActionDelegate {

	public void run(IAction arg0) {
		PyPropertyTraceDialog dialog = new PyPropertyTraceDialog(
				getShell());
		dialog.setTitle("Enable/Disable Step Into properties");
		if (dialog.open() == PyPropertyTraceDialog.OK) {
			PyPropertyTraceManager.getInstance().setPyPropertyTraceState(
					dialog.isDisableStepIntoProperties(),
					dialog.isDisableStepIntoGetter(),
					dialog.isDisableStepIntoSetter(),
					dialog.isDisableStepIntoDeleter());
		}
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow arg0) {
	}
}
