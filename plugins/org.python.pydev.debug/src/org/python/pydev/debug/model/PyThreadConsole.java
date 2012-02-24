package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IProcess;

/**
 * A specialisation of PyThread that can't be "controlled" by the user.
 */
public class PyThreadConsole extends PyThread {

	public PyThreadConsole(AbstractDebugTarget target) {
		super(target, "console_main", "console_main");
	}

	@Override
	public boolean canResume() {
		return false;
	}

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return false;
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean canSuspend() {
		return false;
	}

	@Override
	public String getName() throws DebugException {
		IProcess process = getDebugTarget().getProcess();
		return process.getLabel();
	}
}
