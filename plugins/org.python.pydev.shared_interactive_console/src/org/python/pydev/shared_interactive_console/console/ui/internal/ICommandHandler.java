/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui.internal;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;

public interface ICommandHandler {

    void handleCommand(String userInput, ICallback<Object, InterpreterResponse> onResponseReceived);

    public ICompletionProposal[] getTabCompletions(String commandLine, int cursorPosition);

    void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived);

    void beforeHandleCommand(String userInput, ICallback<Object, InterpreterResponse> onResponseReceived);
}
