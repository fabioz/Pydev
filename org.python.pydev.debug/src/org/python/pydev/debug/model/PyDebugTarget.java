/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
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
public class PyDebugTarget implements IDebugTarget, ILaunchListener {

	ILaunch launch;
	IProcess process;
	RemoteDebugger debugger;
	IPath file;
	IThread[] threads;

	public PyDebugTarget(ILaunch launch, 
							IProcess process, 
							IPath file, 
							RemoteDebugger debugger) {
		this.launch = launch;
		this.process = process;
		this.file = file;
		this.debugger = debugger;
		this.threads = new IThread[0];
		launch.addDebugTarget(this);
		debugger.setTarget(this);
		// we have to know when we get removed, so that we can shut off the debugger
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	public void launchRemoved(ILaunch launch) {
		// shut down the remote debugger when parent launch
		if (launch == this.launch) {
			debugger.dispose();
			debugger = null;
		}
	}

	public void launchAdded(ILaunch launch) {
		// noop
	}

	public void launchChanged(ILaunch launch) {
		// noop		
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
		if (file != null)
			return file.lastSegment();
		else
			return "unknown";
	}

	public boolean canTerminate() {
		// We can always terminate, it makes no harm
		return true;
	}

	public boolean isTerminated() {
		return process.isTerminated();
	}

	public void terminate() throws DebugException {
		process.terminate();
	}
	
	public boolean canDisconnect() {
		// TODO NOW Auto-generated method stub
		return true;
	}

	public void disconnect() throws DebugException {
		// TODO Auto-generated method stub

	}

	public boolean isDisconnected() {
		// TODO NOW Auto-generated method stub
		return false;
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

	public IThread[] getThreads() throws DebugException {
		if (threads == null) {
			ListThreadsCommand cmd = new ListThreadsCommand(debugger, this);
			debugger.postCommand(cmd);
			try {
				cmd.waitUntilDone(1000);
				threads = cmd.getThreads();
			} catch (InterruptedException e) {
				threads = new IThread[0];
			}
		}
		return threads;
	}

	public boolean hasThreads() throws DebugException {
		return true;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		// TODO Auto-generated method stub
		return false;
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
		else if (adapter.equals(IResource.class)) {
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(file);
			if (files != null && files.length > 0)
				return files[0];
			else
				return null;
		}
		else
			System.err.println("Need adapter " + adapter.toString());
		return null;
	}

	/**
	 * When a command that originates from daemon is received,
	 * this routine processes it.
	 * The responses to commands originating from here
	 * are processed by commands themselves
	 */
	public void processCommand(String sCmdCode, String sSeqCode, String payload) {
		int cmdCode = Integer.parseInt(sCmdCode);
		int seqCode = Integer.parseInt(sSeqCode);
		if (cmdCode == RemoteDebuggerCommand.CMD_THREAD_CREATED)
			processThreadCreated(payload);
		else
			PydevDebugPlugin.log(IStatus.WARNING, "Unexpected debugger command" + sCmdCode, null);	
	}

	private void fireEvent(DebugEvent event) {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{event});
		}
	}

	private void processThreadCreated(String payload) {
		IThread[] newThreads = ModelUtils.ThreadsFromXML(this, payload);
		if (threads == null)
			threads = newThreads;
		else {
			IThread[] combined = new IThread[threads.length + newThreads.length];
			int i = 0;
			for (i = 0; i < threads.length; i++)
				combined[i] = threads[i];
			for (int j = 0; j < newThreads.length; i++, j++)
				combined[i] = newThreads[j];
			threads = combined;
		}
		// Now notify debugger that new threads were added
		for (int i =0; i< newThreads.length; i++) 
			fireEvent(new DebugEvent(newThreads[i], DebugEvent.CREATE));
	}
}
