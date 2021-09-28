package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class StopOnStartCommand extends AbstractDebuggerCommand {

    private final boolean stop;

    public StopOnStartCommand(AbstractDebugTarget debugger, boolean stop) {
        super(debugger);
        this.stop = stop;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(AbstractDebuggerCommand.CMD_STOP_ON_START, sequence, Boolean.toString(stop));
    }

}
