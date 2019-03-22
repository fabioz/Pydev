package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class SetJinja2ExceptionBreakpointCommand extends AbstractDebuggerCommand {

    private boolean traceJinja2Exception;

    public SetJinja2ExceptionBreakpointCommand(AbstractDebugTarget debugger, boolean traceJinja2Exception) {
        super(debugger);
        this.traceJinja2Exception = traceJinja2Exception;
    }

    @Override
    public String getOutgoing() {
        if (traceJinja2Exception) {
            return makeCommand(CMD_ADD_EXCEPTION_BREAK, sequence, "jinja2-Exception\t2\t0\t0");
        } else {
            return makeCommand(CMD_REMOVE_EXCEPTION_BREAK, sequence, "jinja2-Exception");
        }
    }

}
