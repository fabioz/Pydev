/*
 * Author: atotic
 * Created on May 6, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

/**
 * Set breakpoint command.
 */
public class SetBreakpointCommand extends AbstractDebuggerCommand {

	public String file;
	public Object line;
	public String condition;

	public SetBreakpointCommand(AbstractRemoteDebugger debugger, String file, Object line, String condition) {
		super(debugger);
		this.file = file;
		this.line = line;
		if (condition == null)
			this.condition = "None";
		else
			this.condition = condition;
	}

	public String getOutgoing() {
		return makeCommand(CMD_SET_BREAK, sequence, file + "\t" + line.toString() + "\t" + condition);
	}

}
