package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.SmartStepIntoVariant;
import org.python.pydev.debug.model.XMLUtils;

public class GetSmartStepIntoVariants extends AbstractDebuggerCommand {

    boolean done;
    private String frameId;
    private String threadId;
    private int startLine;
    private int endLine;
    private SmartStepIntoVariant[] response;

    public GetSmartStepIntoVariants(AbstractDebugTarget target, String threadId, String frameId, int startLine,
            int endLine) {
        super(target);
        done = false;
        this.threadId = threadId;
        this.frameId = frameId;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public void waitUntilDone(int timeout) throws InterruptedException {
        while (!done && timeout > 0) {
            timeout -= 50;
            synchronized (this) {
                Thread.sleep(50);
            }
        }
        if (timeout < 0) {
            throw new InterruptedException();
        }
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_GET_SMART_STEP_INTO_VARIANTS, sequence,
                threadId + "\t" + frameId + "\t" + startLine + "\t" + endLine);
    }

    @Override
    public boolean needResponse() {
        return true;
    }

    /**
     * The response is a list of threads
     */
    @Override
    public void processOKResponse(int cmdCode, String payload) {
        if (cmdCode != CMD_GET_SMART_STEP_INTO_VARIANTS) {
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to CMD_GET_SMART_STEP_INTO_VARIANTS" + payload,
                    null);
            return;
        }
        try {
            response = XMLUtils.SmartStepIntoTargetsFromXML(target, payload);
        } catch (CoreException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an unexpected response " + payload, null);
            Log.log(e);
        }
        done = true;
    }

    @Override
    public void processErrorResponse(int cmdCode, String payload) {
        PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an error " + payload, null);
    }

    public SmartStepIntoVariant[] getResponse() {
        return response;
    }
}
