/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * GetVariable network command.
 * 
 * GetVariable gets the value of the variable from network as XML.
 * The caller can busy-wait for the response.
 */
public class EvaluateExpressionCommand extends AbstractDebuggerCommand {

	String locator;
	String expression;
	
	boolean isError = false;
	int responseCode;
	String payload;
	private boolean doExec;

	
	public EvaluateExpressionCommand(AbstractRemoteDebugger debugger, String expression, String locator, boolean doExec) {
		super(debugger);
		this.doExec = doExec;
		this.locator = locator;
		this.expression = expression.replaceAll("\r","").replaceAll("\n","");
	}

	public String getOutgoing() {
		int cmd = CMD_EVALUATE_EXPRESSION;
		if(doExec){
			cmd = CMD_EXEC_EXPRESSION;
		}
		return makeCommand(cmd, sequence, locator + "\t" + expression);
	}

	public boolean needResponse() {
		return true;
	}

	public void processOKResponse(int cmdCode, String payload) {
		responseCode = cmdCode;
		if (cmdCode == CMD_EVALUATE_EXPRESSION || cmdCode == CMD_EXEC_EXPRESSION)
			this.payload = payload;
		else {
			isError = true;
			PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to EvaluateExpressionCommand", null);
		}
	}
	
	public void processErrorResponse(int cmdCode, String payload) {
		responseCode = cmdCode;
		this.payload = payload;
		isError = true;
	}
		
	public String getResponse() throws CoreException {
		if (isError) 
			throw new CoreException(
							PydevDebugPlugin.makeStatus(IStatus.ERROR,
							"pydevd error:" + payload , 
							 null));
		else
			return payload;
	}
}

