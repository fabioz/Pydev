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
        //no-op (previously we got the output from here, but it's now asynchronous and added through
        //onStdoutContentsReceived and onStderrContentsReceived).
    }

    public void userRequest(String text, ScriptConsolePrompt prompt) {
        session.append(prompt.toString());
        session.append(text);
        session.append('\n');
    }

    @Override
    public String toString() {
        return session.toString();
    }

    @Override
    public void onStdoutContentsReceived(String o1) {
        session.append(o1);
    }

    @Override
    public void onStderrContentsReceived(String o2) {
        session.append(o2);
    }
}
