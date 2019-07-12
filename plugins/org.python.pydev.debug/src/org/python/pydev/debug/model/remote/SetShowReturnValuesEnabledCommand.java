package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.string.StringUtils;

public class SetShowReturnValuesEnabledCommand extends AbstractDebuggerCommand {

    private boolean enable;

    public SetShowReturnValuesEnabledCommand(AbstractDebugTarget debugger, boolean enable) {
        super(debugger);
        this.enable = enable;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(AbstractDebuggerCommand.CMD_SHOW_RETURN_VALUES, sequence,
                StringUtils.join("\t", new String[] { "CMD_SHOW_RETURN_VALUES", enable ? "1" : "0" }));
    }

}
