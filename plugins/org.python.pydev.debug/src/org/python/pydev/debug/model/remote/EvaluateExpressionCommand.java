/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 30, 2004
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * GetVariable network command.
 * 
 * GetVariable gets the value of the variable from network as XML.
 * The caller can busy-wait for the response.
 */
public class EvaluateExpressionCommand extends AbstractDebuggerCommand {

    String locator;
    String expression;

    boolean isError = false;
    int responseCode;
    String payload;
    private boolean doExec;

    public EvaluateExpressionCommand(AbstractDebugTarget debugger, String expression, String locator, boolean doExec) {
        super(debugger);
        this.doExec = doExec;
        this.locator = locator;
        this.expression = StringUtils.removeNewLineChars(expression);
    }

    public String getOutgoing() {
        int cmd = CMD_EVALUATE_EXPRESSION;
        if (doExec) {
            cmd = CMD_EXEC_EXPRESSION;
        }
        return makeCommand(cmd, sequence, locator + "\t" + expression);
    }

    public boolean needResponse() {
        return true;
    }

    public void processOKResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        if (cmdCode == CMD_EVALUATE_EXPRESSION || cmdCode == CMD_EXEC_EXPRESSION)
            this.payload = payload;
        else {
            isError = true;
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to EvaluateExpressionCommand", null);
        }
    }

    public void processErrorResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        this.payload = payload;
        isError = true;
    }

    public String getResponse() throws CoreException {
        if (isError)
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "pydevd error:" + payload, null));
        else
            return payload;
    }
}
