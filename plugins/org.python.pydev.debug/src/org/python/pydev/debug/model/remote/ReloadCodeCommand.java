package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Debugger reload command.
 */
public class ReloadCodeCommand extends AbstractDebuggerCommand {

    private String moduleName;
    
    public ReloadCodeCommand(AbstractDebugTarget debugger, String moduleName) {
        super(debugger);
        this.moduleName = moduleName;
    }

    public String getOutgoing() {
        return makeCommand(CMD_RELOAD_CODE, sequence, this.moduleName);
    }

}
