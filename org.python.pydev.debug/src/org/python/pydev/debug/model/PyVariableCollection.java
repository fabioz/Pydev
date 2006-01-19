/*
 * Author: atotic
 * Created on May 4, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

/**
 * PyVariableCollection represents container variables.
 * 
 * It knows how to fetch its contents over the network.
 * 
 */
public class PyVariableCollection extends PyVariable implements ICommandResponseListener, IVariableLocator {

	IVariableLocator locator;
	PyVariable[] variables = new PyVariable[0];
	IVariable[] waitVariables = null;
	int requestedVariables = 0; // Network request state: 0 did not request, 1 requested, 2 requested & arrived
	boolean fireChangeEvent = true;
	
	public PyVariableCollection(AbstractDebugTarget target, String name, String type, String value, IVariableLocator locator) {
		super(target, name, type, value);
		this.locator = locator;
	}
	
	public String getDetailText() throws DebugException {
		return super.getDetailText();
	}

	public String getPyDBLocation() {
		return locator.getPyDBLocation() + "\t" + name;
	}

	private IVariable[] getWaitVariables() {
		if (waitVariables == null) {
			PyVariable waitVar = new PyVariable(target, "wait", "", "for network");
			waitVariables = new IVariable[1];
			waitVariables[0] = waitVar;
		}
		return waitVariables;
	}

	/**
	 * Received when the command has been completed.
	 */
	public void commandComplete(AbstractDebuggerCommand cmd) {
		variables = getCommandVariables(cmd);
		
		requestedVariables = 2;
		if (fireChangeEvent){
			target.fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.STATE));
		}
	}

	/**
	 * @return a list of variables resolved for some command
	 */
	public PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd) {
		PyVariable[] tempVariables = new PyVariable[0];
		try {
			String payload = ((GetVariableCommand) cmd).getResponse();
			tempVariables = XMLUtils.XMLToVariables(target, this, payload);
		} catch (CoreException e) {
			tempVariables = new PyVariable[1];
			tempVariables[0] = new PyVariable(target, "Error", "pydev ERROR", "Could not resolve variable");
			PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
		}
		return tempVariables;
	}

	public IVariable[] getVariables() throws DebugException {
		if (requestedVariables == 2){
			return variables;
		} else if (requestedVariables == 1){
			return getWaitVariables();
		}

		AbstractRemoteDebugger dbg = getDebugger();
		// send the command, and then busy-wait
		GetVariableCommand cmd = getVariableCommand(dbg);
		cmd.setCompletionListener(this);
		requestedVariables = 1;
		fireChangeEvent = false;	// do not fire change event while we are waiting on response
		dbg.postCommand(cmd);
		try {
			// VariablesView does not deal well with children changing asynchronously.
			// it causes unneeded scrolling, because view preserves selection instead
			// of visibility.
			// I try to minimize the occurence here, by giving pydevd time to complete the
			// task before we are forced to do asynchronous notification.
			int i = 10; 
			while (--i > 0 && requestedVariables != 2)
				Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		fireChangeEvent = true;
		if (requestedVariables == 2)
			return variables;
		else
			return getWaitVariables();
	}

	public AbstractRemoteDebugger getDebugger() {
		return target.getDebugger();
	}
	
	public GetVariableCommand getVariableCommand(AbstractRemoteDebugger dbg) {
		return new GetVariableCommand(dbg, getPyDBLocation());
	}

	public boolean hasVariables() throws DebugException {
		return true;
	}
	
	public String getReferenceTypeName() throws DebugException {
		return type;
	}
}
