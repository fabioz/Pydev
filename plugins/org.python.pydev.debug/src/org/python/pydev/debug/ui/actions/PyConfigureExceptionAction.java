package org.python.pydev.debug.ui.actions;

import java.util.Arrays;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.FileUtils;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.SendPyExceptionCommand;
import org.python.pydev.debug.ui.PyConfigureExceptionDialog;
import org.python.pydev.editor.actions.PyAction;

public class PyConfigureExceptionAction extends PyAction implements
		IWorkbenchWindowActionDelegate {

	public void run(IAction action) {

		PyConfigureExceptionDialog dialog = new PyConfigureExceptionDialog(
				getShell(), "", new PyExceptionListProvider(),
				new LabelProvider(), "");

		dialog.setInitialElementSelections(FileUtils
				.getConfiguredExceptions(Constants.EXCEPTION_FILE_NAME));
		dialog.setTitle("Add Python Exception Breakpoint");
		dialog.open();

		Object[] selectedItems = dialog.getResult();
		if (selectedItems != null) {
			String[] exceptionArray = Arrays.copyOf(selectedItems,
					selectedItems.length, String[].class);

			// TODO: Compare last list to that of the current list before saving
			// and posting to debugger
			FileUtils.saveConfiguredExceptions(exceptionArray,
					Constants.EXCEPTION_FILE_NAME);
			AbstractDebugTarget pyDebugTarget = PyExceptionBreakPointManager
					.getInstance().getPyDebugTarget();
			if (selectedItems.length > 0 && pyDebugTarget != null) {
				// Sending python exceptions to the debugger
				SendPyExceptionCommand sendCmd = new SendPyExceptionCommand(
						pyDebugTarget,
						AbstractDebuggerCommand.CMD_SET_PY_EXCEPTION);
				pyDebugTarget.postCommand(sendCmd);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}
}
