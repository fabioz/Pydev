/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

/**
 * KILL_THREAD debugger command
 * 
 */
public class ThreadKillCommand extends AbstractDebuggerCommand {

	String thread_id;
	
	public ThreadKillCommand(RemoteDebugger debugger, String thread_id) {
		super(debugger);
		this.thread_id = thread_id;	
	}
	
	public String getOutgoing() {
		return makeCommand(Integer.toString(CMD_THREAD_KILL), sequence, thread_id);
	}

}
