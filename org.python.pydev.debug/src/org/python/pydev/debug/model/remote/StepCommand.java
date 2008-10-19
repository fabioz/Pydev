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

    int command_id;
    String thread_id;
    
    /**
     * 
     * @param command_id CMD_STEP_INTO CMD_STEP_OVER CMD_STEP_RETURN
     */
    public StepCommand(AbstractDebugTarget debugger, int command_id, String thread_id) {
        super(debugger);
        this.command_id = command_id;
        this.thread_id = thread_id;
    }

    public String getOutgoing() {
        return makeCommand(command_id, sequence, thread_id);
    }

}
