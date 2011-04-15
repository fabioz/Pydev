package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.core.FileUtils;
import org.python.pydev.debug.model.AbstractDebugTarget;

public class SendPyExceptionCommand extends AbstractDebuggerCommand {

	int commandId;

	public SendPyExceptionCommand(AbstractDebugTarget debugger, int commandId) {
		super(debugger);
		this.commandId = commandId;
	}

	@Override
	public String getOutgoing() {
		String pyExceptions = "";
		pyExceptions = FileUtils.readExceptionsFromFile();
		return makeCommand(commandId, sequence, pyExceptions);
	}
}
