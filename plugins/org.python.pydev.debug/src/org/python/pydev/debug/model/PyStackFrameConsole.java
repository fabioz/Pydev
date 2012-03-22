package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugException;

public class PyStackFrameConsole extends PyStackFrame {

	public PyStackFrameConsole(PyThread in_thread, AbstractDebugTarget target) {
		super(in_thread, "1", "frame_main", null, -1, target);
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
		// This matches hard coded __main__ in pydevconsole.py
		return "__main__";
	}

	public int hashCode() {
		return getThread().hashCode();
	}

	public boolean equals(Object obj) {
		// All PyStackFrame Consoles look the same, so they are only equal if
		// they are identical
		return this == obj;
	}

}
