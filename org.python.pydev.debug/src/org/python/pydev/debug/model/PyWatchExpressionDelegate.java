/*
 * Author: tusk
 * Created: Dec 05, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.EvaluateExpressionCommand;
import org.python.pydev.debug.model.remote.RemoteDebugger;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

public class PyWatchExpressionDelegate 
	implements IWatchExpressionDelegate, IWatchExpressionResult,
	ICommandResponseListener {
	
	protected PyVariable variables[] = new PyVariable[0];
	protected IDebugElement context;
	protected String expression;
	protected IWatchExpressionListener listener;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionDelegate#evaluateExpression(java.lang.String, org.eclipse.debug.core.model.IDebugElement, org.eclipse.debug.core.model.IWatchExpressionListener)
	 */
	public void evaluateExpression(String expression, IDebugElement context, IWatchExpressionListener listener) {
		this.expression = expression;
		this.context = context;
		this.listener = listener;
		if (context instanceof PyStackFrame) {
			
			RemoteDebugger dbg;
			dbg = ((PyDebugTarget)context.getDebugTarget()).getDebugger();
			
			// send the command, and then busy-wait
			EvaluateExpressionCommand cmd = new EvaluateExpressionCommand( 
				dbg, expression,
				((PyStackFrame)context).getLocalsLocator().getPyDBLocation() );
			cmd.setCompletionListener(this);
			dbg.postCommand(cmd);
			
		}else{
			addError( "unknown expression context" );
			listener.watchEvaluationFinished( this );
		}
	}
	
	protected String errors[] = new String[0];

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionResult#getValue()
	 */
	public IValue getValue() {
		synchronized(variables) {
			if (variables.length == 0) {
				variables = new PyVariable[1];
				variables[0] = new PyVariable(
					(PyDebugTarget)context.getDebugTarget(), "Error", "pydev ERROR", "Could not resolve variable");
			}
			return variables[0];
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionResult#hasErrors()
	 */
	public boolean hasErrors() {
		return errors.length > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionResult#getErrorMessages()
	 */
	public String[] getErrorMessages() {
		return errors;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionResult#getExpressionText()
	 */
	public String getExpressionText() {
		return expression;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IWatchExpressionResult#getException()
	 */
	public DebugException getException() {
		return null;
	}
	
	public void addError( String error_string ) {
		String resized_errors[] = new String[errors.length + 1];
		for( int i = 0; i < errors.length; i++ ) {
			resized_errors[i] = errors[i];
		}
		errors = resized_errors;
		errors[errors.length-1] = error_string;
	}

	/* (non-Javadoc)
	 * @see org.python.pydev.debug.model.remote.ICommandResponseListener#commandComplete(org.python.pydev.debug.model.remote.AbstractDebuggerCommand)
	 */
	public void commandComplete(AbstractDebuggerCommand cmd) {		
		try {
			String payload = ((EvaluateExpressionCommand) cmd).getResponse();
			synchronized(variables) {
				variables = XMLUtils.XMLToVariables(
					(PyDebugTarget)context.getDebugTarget(), 
					((PyStackFrame)context).getLocalsLocator(), payload);
			}
		} catch (CoreException e) {
			synchronized(variables) {
				variables = new PyVariable[1];
				variables[0] = new PyVariable(
					(PyDebugTarget)context.getDebugTarget(), "Error", "pydev ERROR", "Could not resolve variable");
			}
			PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
		}
		synchronized(variables) {
			if (variables[0] instanceof PyVariableCollection) {
				((PyVariableCollection)variables[0]).requestedVariables = 2;
			}
		}
		
		listener.watchEvaluationFinished( this );		
	}
}
