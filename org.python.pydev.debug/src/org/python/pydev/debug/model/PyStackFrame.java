/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * Represents a stack entry.
 * 
 * Needs to integrate with the source locator
 */
public class PyStackFrame extends PlatformObject implements IStackFrame {

	private String name;
	private PyThread thread;
	private String id;
	private IPath path;
	private int line;
	private IVariable[] variables;
	private IVariableLocator localsLocator;
	private IVariableLocator globalsLocator;

	public PyStackFrame(PyThread in_thread, String in_id, String name, IPath file, int line) {
		this.id = in_id;
		this.name = name;
		this.path = file;
		this.line = line;
		this.thread = in_thread;
		localsLocator = new IVariableLocator() {
			public String getPyDBLocation() {
				return thread.getId() + "\t" + id + "\tLOCAL"; 
			}
		};
		globalsLocator = new IVariableLocator() {
			public String getPyDBLocation() {
				return thread.getId() + "\t" + id + "\tGLOBAL"; 
			}
		};
	}

	public String getId() {
		return id;
	}
	
	public IVariableLocator getLocalsLocator() {
		return localsLocator;
	}
	
	public IVariableLocator getGlobalLocator() {
		return globalsLocator;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setPath(IPath path) {
		this.path = path;
	}
	
	public void setLine(int line) {
		this.line = line;
	}

	public IPath getPath() {
		return path;
	}
	
	public IThread getThread() {
		return thread;
	}
	
	public void setVariables(IVariable[] locals) {
		this.variables = locals;
	}
	
	public IVariable[] getVariables() throws DebugException {
		return variables;
	}

	public boolean hasVariables() throws DebugException {
		return (variables != null);
	}

	public int getLineNumber() throws DebugException {
		return line;
	}

	public int getCharStart() throws DebugException {
		return -1;
	}

	public int getCharEnd() throws DebugException {
		return -1;
	}

	public String getName() throws DebugException {
		return name + " [" + path.lastSegment() + ":" + Integer.toString(line) + "]";
//		return "(" + id + ")" + name + " [" + path.lastSegment() + ":" + Integer.toString(line) + "]";
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
		else if (adapter.equals(IPropertySource.class) 
			|| adapter.equals(ITaskListResourceAdapter.class)
			|| adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)
			|| adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)
			)
			return  super.getAdapter(adapter);
		// ongoing, I do not fully understand all the interfaces they'd like me to support
		System.err.println("PyStackFrame Need adapter " + adapter.toString());
		return super.getAdapter(adapter);
	}

	/**
	 * HACK
	 * Here to work around eclipse2 annotation marker removal code.
	 * This makes sure that old stack markers get removed.
	 * E3 remove me in Eclipse 3
	 */
	public int hashCode() {
		return 5;
	}
	/**
	 * HACK
	 * Here to work around eclipse2 annotation marker removal code
	 * E3 remove me in Eclipse 3
	 * LaunchView wants to know, 
	 */
	public boolean equals(Object obj) {
		if (obj instanceof PyStackFrame) 
			try {
				return path.equals(((PyStackFrame)obj).getPath()) && 
						(line == ((PyStackFrame)obj).getLineNumber());
			} catch (DebugException e) {
				PydevDebugPlugin.log(IStatus.ERROR, "PyStackFrame.equals", null);
				return false;
			}
		else
			return super.equals(obj);
	}
}
