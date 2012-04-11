package org.python.pydev.debug.newconsole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleFactory;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.editor.actions.PyAction;

/**
 * User can also launch pydev/debug console using Debug view context menu 
 * 
 * @author hussain.bohra
 *
 */
public class DebugConsoleAction extends PyAction{

	// Initialize the console factory class
	private static final IConsoleFactory fFactory = new PydevConsoleFactory();

	public void run(IAction action) {
		try {
            fFactory.openConsole();
        } catch (Exception e) {
            ConsolePlugin.log(e);
        }
	}
}
