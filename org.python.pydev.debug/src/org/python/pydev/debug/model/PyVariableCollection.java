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
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.model.remote.RemoteDebugger;

/**
 *
 * TODO Comment this class
 */
public class PyVariableCollection extends PyVariable implements ICommandResponseListener, IVariableLocator {

	IVariableLocator locator;
	PyVariable[] variables = new PyVariable[0];
	IVariable[] waitVariables = null;
	int requestedVariables = 0; // Network request state: 0 did not request, 1 requested, 2 requested & arrived
	boolean fireChangeEvent = true;
	
	public PyVariableCollection(PyDebugTarget target, String name, String type, String value, IVariableLocator locator) {
		super(target, name, type, value);
		this.locator = locator;
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

	public void commandComplete(AbstractDebuggerCommand cmd) {
		try {
			String payload = ((GetVariableCommand) cmd).getResponse();
			synchronized(variables) {
				variables = XMLUtils.XMLToVariables(target, this, payload);
			}
		} catch (CoreException e) {
			variables = new PyVariable[1];
			variables[0] = new PyVariable(target, "Error", "pydev ERROR", "Could not resolve variable");
			PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
		}
		requestedVariables = 2;
		if (fireChangeEvent)
			target.fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.STATE));
	}

	public IVariable[] getVariables() throws DebugException {
		if (requestedVariables == 2)
			return variables;
		else if (requestedVariables == 1)
			return getWaitVariables();

		RemoteDebugger dbg;
		dbg = target.getDebugger();
		
		// send the command, and then busy-wait
		GetVariableCommand cmd = new GetVariableCommand(dbg, getPyDBLocation());
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

	public boolean hasVariables() throws DebugException {
		return true;
	}
	
	public String getReferenceTypeName() throws DebugException {
		return type;
	}
}
