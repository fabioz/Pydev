/*
 * Author: atotic
 * Created on Apr 21, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.XMLUtils;

/**
 * ListThreads command.
 * 
 * See protocol for more info
 */
public class ThreadListCommand extends AbstractDebuggerCommand {

	boolean done;
	PyDebugTarget target;
	IThread[] threads;
	
	public ThreadListCommand(RemoteDebugger debugger, PyDebugTarget target) {
		super(debugger);
		this.target = target;
		done = false;
	}

	public void waitUntilDone(int timeout) throws InterruptedException {
		while (!done && timeout > 0) {
			timeout -= 100;
			Thread.sleep(100);
		}
		if (timeout < 0)
			throw new InterruptedException();
	}
	
	public IThread[] getThreads() {
		return threads;
	}
	
	public String getOutgoing() {
		return makeCommand(CMD_LIST_THREADS, sequence, "");
	}
	
	public boolean needResponse() {
		return true;
	}
	
	/**
	 * The response is a list of threads
	 */
	public void processOKResponse(int cmdCode, String payload) {
		if (cmdCode != 102) {
			PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to LIST THREADS"  + payload, null);
			return;
		}
		try {
			threads = XMLUtils.ThreadsFromXML(target, payload);
		} catch (CoreException e) {
			PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an unexpected response "  + payload, null);
			e.printStackTrace();
		}
		done = true;
	}

	
	public void processErrorResponse(int cmdCode, String payload) {
		PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an error "  + payload, null);
	}
}
