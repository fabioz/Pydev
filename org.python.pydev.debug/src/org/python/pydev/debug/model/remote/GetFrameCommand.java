package org.python.pydev.debug.model.remote;

public class GetFrameCommand extends GetVariableCommand{

    public GetFrameCommand(AbstractRemoteDebugger debugger, String locator) {
        super(debugger, locator);
    }
    
    @Override
    protected int getCommandId() {
        return CMD_GET_FRAME;
    }


}
