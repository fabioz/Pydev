/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

/**
 * Represents a stack entry.
 * 
 * Needs to integrate with the source locator
 */
public class PyStackFrame implements IStackFrame {

	private String name;
	private PyThread thread;
	private String id;
	private IPath path;
	private int line;
	
	public PyStackFrame(String id, String name, IPath file, int line) {
		this.id = id;
		this.name = name;
		this.path = file;
		this.line = line;
	}

	public IPath getPath() {
		return path;
	}
	
	public IThread getThread() {
		return thread;
	}

	public void setThread(PyThread thread) {
		this.thread = thread;
	}
	
	public IVariable[] getVariables() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasVariables() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getLineNumber() throws DebugException {
		return line;
	}

	public int getCharStart() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getCharEnd() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getName() throws DebugException {
		return name + " [" + path.lastSegment() + ":" + Integer.toString(line) + "]";
	}

	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	public String getModelIdentifier() {
		return thread.getModelIdentifier();
	}

	public IDebugTarget getDebugTarget() {
		return thread.getDebugTarget();
	}

	public ILaunch getLaunch() {
		return thread.getLaunch();
	}

	public boolean canStepInto() {
		return thread.canStepInto();
	}

	public boolean canStepOver() {
		return thread.canStepOver();
	}

	public boolean canStepReturn() {
		return thread.canStepReturn();
	}

	public boolean isStepping() {
		return thread.isStepping();
	}

	public void stepInto() throws DebugException {
		thread.stepInto();
	}

	public void stepOver() throws DebugException {
		thread.stepOver();
	}

	public void stepReturn() throws DebugException {
		thread.stepReturn();
	}

	public boolean canResume() {
		return thread.canResume();
	}

	public boolean canSuspend() {
		return thread.canSuspend();
	}

	public boolean isSuspended() {
		return thread.isSuspended();
	}

	public void resume() throws DebugException {
		thread.resume();
	}

	public void suspend() throws DebugException {
		thread.suspend();
	}

	public boolean canTerminate() {
		return thread.canTerminate();
	}

	public boolean isTerminated() {
		return thread.isTerminated();
	}

	public void terminate() throws DebugException {
		thread.terminate();
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(ILaunch.class) ||
			adapter.equals(IResource.class))
			return thread.getAdapter(adapter);
		else if (adapter.equals(ITaskListResourceAdapter.class))
			return null;
		// ongoing, I do not fully understand all the interfaces they'd like me to support
		System.err.println("PyStackFrame Need adapter " + adapter.toString());
		return null;
	}

}
