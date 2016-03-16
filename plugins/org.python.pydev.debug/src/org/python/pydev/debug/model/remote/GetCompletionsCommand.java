/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Gets the completions for a given stack (locals and globals in a suspended context).
 */
public class GetCompletionsCommand extends AbstractDebuggerCommand {

    private String actTok;
    private String locator;
    private boolean isError = false;
    private int responseCode;
    private String payload;

    public GetCompletionsCommand(AbstractDebugTarget debugger, String actTok, String locator) {
        super(debugger);
        this.locator = locator;
        this.actTok = actTok;
    }

    @Override
    public String getOutgoing() {
        int cmd = CMD_GET_COMPLETIONS;
        return makeCommand(cmd, sequence, locator + "\t" + actTok);
    }

    @Override
    public boolean needResponse() {
        return true; //The response are the completions!
    }

    @Override
    public void processOKResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        if (responseCode == CMD_GET_COMPLETIONS)
            this.payload = payload;
        else {
            isError = true;
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to GetCompletionsCommand", null);
        }
    }

    @Override
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
