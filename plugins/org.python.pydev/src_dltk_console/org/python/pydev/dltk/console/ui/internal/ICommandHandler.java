/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui.internal;

import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.dltk.console.InterpreterResponse;

public interface ICommandHandler {

    void handleCommand(String userInput, ICallback<Object, InterpreterResponse> onResponseReceived,
            ICallback<Object, Tuple<String, String>> onContentsReceived);
}
