/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * Superclass of all debugger commands.
 * 
 * Debugger commands know how to interact with pydevd.py.
 * See pydevd.py for protocol information.
 * 
 * Command lifecycle:
 *  cmd = new Command() // creation
 *  cmd.getSequence()	// get the sequence number of the command
 *  cmd.getOutgoing()	// asks command for outgoing message
 *  cmd.aboutToSend()	// called right before we go on wire
 * 						// by default, if command needs response
 * 						// it gets posted to in the response queue
 * 	if (cmd.needsResponse())
 * 		post the command to response queue, otherwise we are done
 *  when response arrives:
 *  if response is an error
 * 		cmd.processResponse()
 * 	else
 * 		cmd.processErrorResponse()
 * 
 */
public abstract class RemoteDebuggerCommand {
	
	static final int CMD_LIST_THREADS = 102;
	static final int CMD_THREAD_CREATED = 103;
	static final int CMD_ERROR = 501;
	static final int CMD_VERSION = 901;
	static final int CMD_RETURN = 902;
	
	protected RemoteDebugger debugger;
	
	public RemoteDebuggerCommand(RemoteDebugger debugger) {
		this.debugger = debugger;
	}

	/**
	 * @return outgoing message
	 */
	public abstract String getOutgoing();
	
	/**
	 * Notification right before the command is sent.
	 * If subclassed, call super()
	 */
	public void aboutToSend() {
		// if we need a response, put me on the waiting queue
		if (needResponse())
			debugger.addToResponseQueue(this);
	}

	/**
	 * Does this command require a response?
	 */
	public boolean needResponse() {
		return false;
	}
	
	/**
	 * returns Sequence # 
	 */
	public int getSequence() {
		System.err.println("Fatal: must override getSequence");
		PydevDebugPlugin.log(IStatus.ERROR, "getSequence must be overridden", null);
		return 0;
	}
	
	/**
	 * notification of the response to the command.
	 * You'll get either processResponse or processErrorResponse
	 */
	public void processResponse(int cmdCode, String payload) {
		PydevDebugPlugin.log(IStatus.ERROR, "Debugger command ignored response " + getClass().toString() + payload, null);
	}
	
	public void processErrorResponse(int cmdCode, String payload) {
		PydevDebugPlugin.log(IStatus.ERROR, "Debugger command ignored error response " + getClass().toString() + payload, null);
	}
	
	public static String makeCommand(String code, int sequence, String payload) {
		StringBuffer s = new StringBuffer();
		s.append(code);
		s.append("\t");
		s.append(sequence);
		s.append("\t");
		s.append(payload);
		return s.toString();
	}
}
