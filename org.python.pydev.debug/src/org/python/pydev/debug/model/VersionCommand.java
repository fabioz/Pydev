/*
 * Author: atotic
 * Created on Apr 19, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

/**
 * Version debugger command.
 * 
 * See protocol definition for more info
 */
public class VersionCommand extends RemoteDebuggerCommand {

	static final String VERSION = "1.0";
	
	int sequence;
	/**
	 * @param debugger
	 */
	public VersionCommand(RemoteDebugger debugger) {
		super(debugger);
		sequence = debugger.getNextSequence();
	}

	public String getOutgoing() {
		return makeCommand(Integer.toString(CMD_VERSION), sequence, VERSION);
	}

	public boolean needResponse() {
		return true;
	}

	public int getSequence() {
		return sequence;
	}
	
	public void processResponse(int cmdCode, String payload) {
		System.err.println("The version is " + payload);
	}

}
