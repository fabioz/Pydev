package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.debug.ui.launching.LaunchShortcut;
import org.python.pydev.editor.actions.PyAction;

public class RunEditorAsPython extends PyAction{

	public void run(IAction action) {
		LaunchShortcut shortcut = new LaunchShortcut();
		shortcut.launch(getPyEdit(), "run");
	}

}
