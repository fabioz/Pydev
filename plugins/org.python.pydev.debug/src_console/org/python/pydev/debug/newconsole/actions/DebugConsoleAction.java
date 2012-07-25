package org.python.pydev.debug.newconsole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.console.ConsolePlugin;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevDebugConsoleFrame;
import org.python.pydev.editor.actions.PyAction;

/**
 * User can also launch pydev/debug console using Debug view context menu
 * 
 * @author hussain.bohra
 */
public class DebugConsoleAction extends PyAction {

    // Initialize the console factory class
    private static final PydevConsoleFactory fFactory = new PydevConsoleFactory();

    public void run(IAction action) {
        try {
            PyStackFrame suspendedFrame = PydevDebugConsoleFrame.getCurrentSuspendedPyStackFrame();
            fFactory.createDebugConsole(suspendedFrame, null);
        } catch (Exception e) {
            ConsolePlugin.log(e);
        }
    }
}
