/*
 * Author: atotic
 * Created on Apr 27, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Debugger step command.
 */
public class StepCommand extends AbstractDebuggerCommand {

    int commandId;
    String threadId;
    
    /**
     * 
     * @param commandId CMD_STEP_INTO CMD_STEP_OVER CMD_STEP_RETURN
     */
    public StepCommand(AbstractDebugTarget debugger, int commandId, String threadId) {
        super(debugger);
        this.commandId = commandId;
        this.threadId = threadId;
    }

    public String getOutgoing() {
        return makeCommand(commandId, sequence, threadId);
    }

}
