/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package com.aptana.js.interactive_console.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.aptana.interactive_console.console.IScriptConsoleShell;
import com.aptana.interactive_console.console.ui.IScriptConsoleViewer;
import com.aptana.interactive_console.console.ui.ScriptConsoleTextHover;
import com.aptana.shared_core.utils.Log;

public class JSConsoleTextHover extends ScriptConsoleTextHover {

    private IScriptConsoleShell interpreterShell;

    public JSConsoleTextHover(IScriptConsoleShell interpreterShell) {
        this.interpreterShell = interpreterShell;
    }

    protected String getHoverInfoImpl(IScriptConsoleViewer viewer, IRegion hoverRegion) {
        try {
            IDocument document = viewer.getDocument();
            int cursorPosition = hoverRegion.getOffset();

            return interpreterShell.getDescription(document, cursorPosition);
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }
}
