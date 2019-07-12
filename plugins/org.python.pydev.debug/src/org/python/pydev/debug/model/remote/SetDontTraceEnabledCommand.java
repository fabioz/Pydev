package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class SetDontTraceEnabledCommand extends AbstractDebuggerCommand {

    private boolean enable;

    public SetDontTraceEnabledCommand(AbstractDebugTarget debugger, boolean enable) {
        super(debugger);
        this.enable = enable;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(AbstractDebuggerCommand.CMD_ENABLE_DONT_TRACE, sequence,
                String.valueOf(enable));
    }

}
