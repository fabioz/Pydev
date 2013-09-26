/******************************************************************************
* Copyright (C) 2012  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
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
