/*
 * Author: atotic
 * Created on Apr 19, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

/**
 * Version debugger command.
 * 
 * See protocol definition for more info. Used as
 */
public class VersionCommand extends AbstractDebuggerCommand {

	static final String VERSION = "1.0";
	
	/**
	 * @param debugger
	 */
	public VersionCommand(RemoteDebugger debugger) {
		super(debugger);
	}

	public String getOutgoing() {
		return makeCommand(Integer.toString(CMD_VERSION), sequence, VERSION);
	}

	public boolean needResponse() {
		return true;
	}
	
	public void processResponse(int cmdCode, String payload) {
		System.err.println("The version is " + payload);
	}

}
