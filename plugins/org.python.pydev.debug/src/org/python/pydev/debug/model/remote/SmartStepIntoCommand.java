package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.SmartStepIntoVariant;

public class SmartStepIntoCommand extends AbstractDebuggerCommand {

    private final int commandId;
    private final String threadId;
    private final String funcName;
    private final int line;
    private final SmartStepIntoVariant stepIntoTarget;

    /**
     * @param command_id CMD_SMART_STEP_INTO
     */
    public SmartStepIntoCommand(AbstractDebugTarget debugger, int command_id, String threadId, int line,
            String funcName, SmartStepIntoVariant stepIntoTarget) {
        super(debugger);
        this.commandId = command_id;
        this.threadId = threadId;
        this.line = line;
        this.funcName = funcName;
        this.stepIntoTarget = stepIntoTarget;
    }

    @Override
    public String getOutgoing() {
        if (stepIntoTarget.childOffset >= 0) {
            return makeCommand(commandId, sequence, threadId + "\toffset=" + stepIntoTarget.offset + ";"
                    + stepIntoTarget.childOffset + "\t" + funcName);
        }
        return makeCommand(commandId, sequence, threadId + "\toffset=" + stepIntoTarget.offset + "\t" + funcName);
    }

}
