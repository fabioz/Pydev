/*
 * Author: atotic
 * Created on Apr 21, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

/**
 * Represents python threads.
 * 
 * 
 */
public class PyThread implements IThread {

	private PyDebugTarget target;
	private String name;
	private String id;
	boolean isPydevThread;	// true if this is a debugger thread, that can't be killed/suspended

	boolean isSuspended = false;
	boolean isStepping = false;
	IStackFrame[] stack;

	public PyThread(PyDebugTarget target, String name, String id) {
		this.target = target;
		this.name = name;
		this.id = id;
		isPydevThread = id.equals("-1");	// use a special id for pydev threads
	}
	
	/**
	 * If a thread is entering a suspended state, pass in the stack
	 */
	public void setSuspended(boolean state, IStackFrame[] stack) {
		isSuspended = state;
		this.stack = stack;
		if (stack != null) 
			for (int i=0; i<stack.length; i++)
				((PyStackFrame)stack[i]).setThread(this);
	}

	public String getName() throws DebugException {
		return name;
	}
	
	public String getId() {
		return id;
	}

	public int getPriority() throws DebugException {
		return 0;
	}

	public String getModelIdentifier() {
		return target.getModelIdentifier();
	}

	public IDebugTarget getDebugTarget() {
		return target;
	}

	public ILaunch getLaunch() {
		return target.getLaunch();
	}

	public boolean canTerminate() {
		return !isPydevThread;
	}

	public boolean isTerminated() {
		return target.isTerminated();
	}

	public void terminate() throws DebugException {
		if (!isPydevThread) {
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new ThreadKillCommand(d, id));
		}
	}

	public boolean canResume() {
		return !isPydevThread && isSuspended;
	}

	public boolean canSuspend() {
		return !isPydevThread && !isSuspended;
	}

	public boolean isSuspended() {
		return isSuspended;
	}

	public void resume() throws DebugException {
		if (!isPydevThread) {
			isStepping = false;
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new ThreadRunCommand(d, id));
		}
	}

	public void suspend() throws DebugException {
		if (!isPydevThread) {
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new ThreadSuspendCommand(d, id));
		}
	}

	public boolean canStepInto() {
		return !isPydevThread && isSuspended;
	}

	public boolean canStepOver() {
		return !isPydevThread && isSuspended;
	}

	public boolean canStepReturn() {
		return !isPydevThread && isSuspended;
	}

	public boolean isStepping() {
		return isStepping;
	}

	public void stepInto() throws DebugException {
		if (!isPydevThread) {
			isStepping = true;
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new StepCommand(d, AbstractDebuggerCommand.CMD_STEP_INTO, id));
		}		
	}

	public void stepOver() throws DebugException {
		if (!isPydevThread) {
			isStepping = true;
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new StepCommand(d, AbstractDebuggerCommand.CMD_STEP_OVER, id));
		}		
	}

	public void stepReturn() throws DebugException {
		if (!isPydevThread) {
			isStepping = true;
			RemoteDebugger d = target.getDebugger();
			d.postCommand(new StepCommand(d, AbstractDebuggerCommand.CMD_STEP_RETURN, id));
		}		
	}

	public IStackFrame[] getStackFrames() throws DebugException {
		return stack;
	}

	public boolean hasStackFrames() throws DebugException {
		return (stack != null && stack.length > 0);
	}

	public IStackFrame getTopStackFrame() throws DebugException {
		return stack == null ? stack[0] : null;
	}

	public IBreakpoint[] getBreakpoints() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(ILaunch.class) ||
			adapter.equals(IResource.class))
			return target.getAdapter(adapter);
		// ongoing, I do not fully understand all the interfaces they'd like me to support
		System.err.println("PythonThread Need adapter " + adapter.toString());
		return null;
	}

}
