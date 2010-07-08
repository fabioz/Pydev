package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.docutils.StringUtils;
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

    public String getOutgoing() {
        int cmd = CMD_GET_COMPLETIONS;
        return makeCommand(cmd, sequence, locator + "\t" + actTok);
    }

    public boolean needResponse() {
        return true; //The response are the completions!
    }

    public void processOKResponse(int cmdCode, String payload) {
        responseCode = cmdCode;
        if (responseCode == CMD_GET_COMPLETIONS)
            this.payload = payload;
        else {
            isError = true;
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to GetCompletionsCommand", null);
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
