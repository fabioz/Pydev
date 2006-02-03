package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.debug.ui.launching.JythonLaunchShortcut;
import org.python.pydev.editor.actions.PyAction;

public class RunEditorAsJython extends PyAction{

	public void run(IAction action) {
		JythonLaunchShortcut shortcut = new JythonLaunchShortcut();
		shortcut.launch(getPyEdit(), "run");
	}

}
