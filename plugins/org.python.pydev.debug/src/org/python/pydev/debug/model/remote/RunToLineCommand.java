package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Debugger run to line command.
 */
public class RunToLineCommand extends AbstractDebuggerCommand {

    int commandId;
    String threadId;
    String funcName;
    int line;
    
    /**
     * @param command_id CMD_RUN_TO_LINE
     */
    public RunToLineCommand(AbstractDebugTarget debugger, int command_id, String threadId, int line, String funcName) {
        super(debugger);
        this.commandId = command_id;
        this.threadId = threadId;
        this.line = line;
        this.funcName = funcName;
    }

    public String getOutgoing() {
        return makeCommand(commandId, sequence, threadId+"\t"+line+"\t"+funcName);
    }

}
