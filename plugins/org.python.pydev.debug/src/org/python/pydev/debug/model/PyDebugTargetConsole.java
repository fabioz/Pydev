package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.RemoteDebuggerConsole;
import org.python.pydev.debug.model.remote.VersionCommand;
import org.python.pydev.debug.newconsole.IPydevConsoleDebugTarget;
import org.python.pydev.debug.newconsole.PydevConsole;
import org.python.pydev.debug.newconsole.PydevConsoleCommunication;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;

public class PyDebugTargetConsole extends PyDebugTarget implements IPydevConsoleDebugTarget {

	PyThreadConsole thread;
	IThread[] threads;
	private final PydevConsoleCommunication scriptConsoleCommunication;
	private ScriptConsole console;

	public PyDebugTargetConsole(PydevConsoleCommunication scriptConsoleCommunication, ILaunch launch, IProcess process,
			RemoteDebuggerConsole debugger) {
		super(launch, process, null, debugger, null);
		this.scriptConsoleCommunication = scriptConsoleCommunication;

		thread = new PyThreadConsole(this);
		threads = new IThread[] { thread };
	}

	@Override
	public RemoteDebuggerConsole getDebugger() {
		return (RemoteDebuggerConsole) super.getDebugger();
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		if (isTerminated())
			return new IThread[0];
		return threads;
	}

	private IStackFrame[] createFrames() {
		PyStackFrameConsole frame = new PyStackFrameConsole(thread, this);
		return new IStackFrame[] { frame };
	}

	public void setSuspended(boolean suspended) {
		if (suspended != thread.isSuspended()) {
			final int state;
			if (suspended) {
				state = DebugEvent.SUSPEND;
				thread.setSuspended(true, createFrames());
			} else {
				state = DebugEvent.RESUME;
				thread.setSuspended(false, null);
			}
			fireEvent(new DebugEvent(thread, state, DebugEvent.CLIENT_REQUEST));
		}
	}

	@Override
	public String getName() throws DebugException {
		if (console == null) {
			return PydevConsole.CONSOLE_NAME;
		}
		return console.getName();
	}

	public void initialize() {
		// we post version command just for fun
		// it establishes the connection
		this.postCommand(new VersionCommand(this));

		// We don't issue run command or anything similar, we just start off
		// suspended
		setSuspended(true);
	}

	@Override
	public void postCommand(AbstractDebuggerCommand cmd) {
		try {
			scriptConsoleCommunication.postCommand(cmd);
		} catch (Exception e) {
			Log.log(e);
		}
	}

	@Override
	public void terminate() {
		super.terminate();
		if (console != null) {
			ScriptConsoleManager.getInstance().close(console);
		}
	}

	public void setConsole(ScriptConsole console) {
		this.console = console;
	}
}
