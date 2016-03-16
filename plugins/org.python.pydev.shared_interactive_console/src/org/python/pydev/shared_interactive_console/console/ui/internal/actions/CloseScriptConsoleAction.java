/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleManager;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

/**
 * Terminate action (shown as the terminate in the console).
 */
public class CloseScriptConsoleAction extends Action {

    private ScriptConsole console;

    public CloseScriptConsoleAction(ScriptConsole console, String text, String tooltip) {
        this.console = console;

        setText(text);
        setToolTipText(tooltip);
    }

    @Override
    public void run() {
        ScriptConsoleManager.getInstance().close(console);
    }

    public void update() {
        setEnabled(true);
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return InteractiveConsolePlugin.getDefault().getImageDescriptor(ScriptConsoleUIConstants.TERMINATE_ICON);
    }
}
