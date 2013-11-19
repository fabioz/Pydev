/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui.internal;

import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleListener;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleSession;

public class ScriptConsoleSession implements IScriptConsoleListener, IScriptConsoleSession {

    private StringBuffer session;

    public ScriptConsoleSession() {
        this.session = new StringBuffer();
    }

    public void interpreterResponse(InterpreterResponse response, ScriptConsolePrompt prompt) {
        if (response != null) {
            if (response.err != null && response.err.length() > 0) {
                session.append(response.err);
            }
            if (response.out != null && response.out.length() > 0) {
                session.append(response.out);
            }
        }
    }

    public void userRequest(String text, ScriptConsolePrompt prompt) {
        session.append(prompt.toString());
        session.append(text);
        session.append('\n');
    }

    public String toString() {
        return session.toString();
    }
}
