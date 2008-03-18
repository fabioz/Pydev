/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.debug.newconsole;

import org.eclipse.jface.text.IRegion;
import org.python.pydev.dltk.console.IScriptConsoleShell;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.dltk.console.ui.ScriptConsoleTextHover;
import org.python.pydev.plugin.PydevPlugin;

public class PydevConsoleTextHover extends ScriptConsoleTextHover {

    private IScriptConsoleShell interpreterShell;

    public PydevConsoleTextHover(IScriptConsoleShell interpreterShell) {
        this.interpreterShell = interpreterShell;
    }

    protected String getHoverInfoImpl(IScriptConsoleViewer viewer, IRegion hoverRegion) {
        try {
            int cursorPosition = hoverRegion.getOffset() - viewer.getCommandLineOffset();

            String commandLine = viewer.getCommandLine();

            return interpreterShell.getDescription(commandLine, cursorPosition);
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        }
    }
}
