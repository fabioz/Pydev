package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyPropertyTraceManager;

public class SetPropertyTraceCommand extends AbstractDebuggerCommand {

	public SetPropertyTraceCommand(AbstractDebugTarget debugger) {
		super(debugger);
	}

	@Override
	public String getOutgoing() {
		PyPropertyTraceManager instance = PyPropertyTraceManager.getInstance();
		String pyPropertyTraceState = instance.getPyPropertyTraceState().trim();
		return makeCommand(AbstractDebuggerCommand.CMD_SET_PROPERTY_TRACE,
				sequence, pyPropertyTraceState);
	}

}
