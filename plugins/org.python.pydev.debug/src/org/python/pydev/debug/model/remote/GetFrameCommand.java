package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class GetFrameCommand extends GetVariableCommand{

    public GetFrameCommand(AbstractDebugTarget debugger, String locator) {
        super(debugger, locator);
    }
    
    @Override
    protected int getCommandId() {
        return CMD_GET_FRAME;
    }


}
