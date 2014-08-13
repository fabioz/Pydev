package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class SetDjangoExceptionBreakpointCommand extends AbstractDebuggerCommand {

    private boolean traceDjangoException;

    public SetDjangoExceptionBreakpointCommand(AbstractDebugTarget debugger, boolean traceDjangoException) {
        super(debugger);
        this.traceDjangoException = traceDjangoException;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(traceDjangoException ? CMD_ADD_DJANGO_EXCEPTION_BREAK : CMD_REMOVE_DJANGO_EXCEPTION_BREAK,
                sequence, "DjangoExceptionBreak");
    }

}
