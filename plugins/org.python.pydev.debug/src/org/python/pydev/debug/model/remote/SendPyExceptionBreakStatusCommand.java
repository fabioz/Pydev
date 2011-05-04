package org.python.pydev.debug.model.remote;

import java.io.File;

import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.FileUtils;
import org.python.pydev.debug.model.AbstractDebugTarget;

public class SendPyExceptionBreakStatusCommand extends AbstractDebuggerCommand {

	int commandId;

	public SendPyExceptionBreakStatusCommand(AbstractDebugTarget debugger, int commandId) {
		super(debugger);
		this.commandId = commandId;
	}

	@Override
	public String getOutgoing() {
		String breakOnUncaught = FileUtils.readExceptionsFromFile(Constants.BREAK_ON_UNCAUGHT_EXCEPTION);
		String breakOnCaught = FileUtils.readExceptionsFromFile(Constants.BREAK_ON_CAUGHT_EXCEPTION);
		
		String breakStatus = breakOnUncaught + File.pathSeparator + breakOnCaught;
		return makeCommand(commandId, sequence, breakStatus);
	}
}