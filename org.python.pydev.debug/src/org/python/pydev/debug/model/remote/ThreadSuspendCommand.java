/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

/**
 * Suspend thread network command.
 * 
 * See protocol docs for more info.
 */
public class ThreadSuspendCommand extends AbstractDebuggerCommand {

	String thread;
	
	public ThreadSuspendCommand(RemoteDebugger debugger, String thread) {
		super(debugger);
		this.thread = thread;
	}

	public String getOutgoing() {
		return makeCommand(CMD_THREAD_SUSPEND, sequence, thread);
	}
}
