/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui.internal;

import org.python.pydev.dltk.console.ui.IScriptConsoleInput;

public class ScriptConsoleInput implements IScriptConsoleInput {

    private ScriptConsolePage page;

    public ScriptConsoleInput(ScriptConsolePage page) {
        this.page = page;
    }

    public void insertText(String line) {
        page.insertText(line);
    }
}
