package org.python.pydev.debug.ui.actions;

import java.util.Arrays;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.LabelProvider;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.FileUtils;
import org.python.pydev.debug.ui.PyConfigureExceptionDialog;
import org.python.pydev.editor.actions.PyAction;

public class PyConfigureException extends PyAction {

	public void run(IAction action) {

		PyConfigureExceptionDialog dialog = new PyConfigureExceptionDialog(
				getShell(), "", new PyExceptionListProvider(getPyEdit()), new LabelProvider(), "");

		dialog.setInitialElementSelections(FileUtils.getConfiguredExceptions(Constants.EXCEPTION_FILE_NAME));
		dialog.setTitle("Add Python Exception Breakpoint");
		dialog.open();

		Object[] selectedItems = dialog.getResult();
		if (selectedItems != null) {
			String[] exceptionArray = Arrays.copyOf(selectedItems,
					selectedItems.length, String[].class);
			FileUtils.saveConfiguredExceptions(exceptionArray);
		}
	}
}
