/*
 * Author: atotic
 * Created on May 6, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

/**
 * Remove breakpoint command
 */
public class RemoveBreakpointCommand extends AbstractDebuggerCommand {

    public String file;
    public Object line;
    
    public RemoveBreakpointCommand(AbstractRemoteDebugger debugger, String file, Object line) {
        super(debugger);
        this.file = file;
        this.line = line;
    }

    public String getOutgoing() {
        return makeCommand(CMD_REMOVE_BREAK, sequence, file + "\t" + line.toString());
    }
}
