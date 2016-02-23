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
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;

public class EvaluateConsoleExpressionCommand extends AbstractDebuggerCommand {

    private boolean isError = false;
    private String locator;
    private int responseCode;
    private String payload;

    public EvaluateConsoleExpressionCommand(AbstractDebugTarget debugger, String locator,
            ICommandResponseListener responseListener) {
        super(debugger);

        this.locator = locator;
        this.responseListener = responseListener;
    }

    @Override
    public String getOutgoing() {
        int cmd = CMD_EVALUATE_CONSOLE_EXPRESSION;
        return makeCommand(cmd, sequence, locator);
    }

    @Override
    public boolean needResponse() {
        return true; // Allows the command to wait till a response is received.
    }

    @Override
    public void processOKResponse(int cmdCode, String payload) {
        this.responseCode = cmdCode;
        if (responseCode == CMD_EVALUATE_CONSOLE_EXPRESSION)
            this.payload = payload;
        else {
            isError = true;
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to GetTaskletCallStackCommand", null);
        }
    }

    @Override
    public void processErrorResponse(int cmdCode, String payload) {
        this.responseCode = cmdCode;
        this.payload = payload;
        isError = true;
    }

    public String getResponse() throws CoreException {
        if (isError)
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, payload, null));
        else
            return payload;
    }

}
