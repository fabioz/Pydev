/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 *
 * TODO Comment this class
 * Make sure we fire the right org.eclipse.debug.core.DebugEvents
 */
public class PythonDebugTarget implements IDebugTarget {

	ILaunch launch;
	IProcess process;
	String name;
	RemoteDebugger debugger;
	
	public PythonDebugTarget(ILaunch launch, IProcess process, String name, RemoteDebugger debugger) {
		this.launch = launch;
		this.process = process;
		this.name = name;
		this.debugger = debugger;
		launch.addDebugTarget(this);
	}

	// From IDebugElement
	public String getModelIdentifier() {
		return PydevDebugPlugin.getPluginID();
	}
	// From IDebugElement
	public IDebugTarget getDebugTarget() {
		return this;
	}
	// From IDebugElement
	public ILaunch getLaunch() {
		return launch;
	}

	public IProcess getProcess() {
		return process;
	}

	public String getName() throws DebugException {
		return name;
	}

	public IThread[] getThreads() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasThreads() throws DebugException {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canTerminate() {
		// TODO NOW Auto-generated method stub
		return true;
	}

	public boolean isTerminated() {
		// TODO NOW Auto-generated method stub
		return false;
	}

	public void terminate() throws DebugException {
		// TODO Auto-generated method stub

	}

	public boolean canResume() {
		// TODO NOW Auto-generated method stub
		return false;
	}

	public boolean canSuspend() {
		// TODO NOW Auto-generated method stub
		return true;
	}

	public boolean isSuspended() {
		// TODO Auto-generated method stub
		return false;
	}

	public void resume() throws DebugException {
		// TODO Auto-generated method stub

	}

	public void suspend() throws DebugException {
		// TODO Auto-generated method stub

	}

	public void breakpointAdded(IBreakpoint breakpoint) {
		// TODO Auto-generated method stub

	}

	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		// TODO Auto-generated method stub

	}

	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// TODO Auto-generated method stub

	}

	public boolean canDisconnect() {
		// TODO NOW Auto-generated method stub
		return false;
	}

	public void disconnect() throws DebugException {
		// TODO Auto-generated method stub

	}

	public boolean isDisconnected() {
		// TODO NOW Auto-generated method stub
		return false;
	}

	public boolean supportsStorageRetrieval() {
		// TODO Auto-generated method stub
		return false;
	}

	public IMemoryBlock getMemoryBlock(long startAddress, long length)
		throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		if (adapter.equals(ILaunch.class))
			return launch;
		else
			System.err.println("Need adapter " + adapter.toString());
		return null;
	}
}
