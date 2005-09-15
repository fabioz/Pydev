package com.python.pydev.debug.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.python.pydev.debug.model.AbstractDebugTarget;

import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class PyDebugTargetServer extends AbstractDebugTarget {
	private static final boolean DEBUG = false;
    private boolean isTerminated;
	
	public PyDebugTargetServer( ILaunch launch, IPath file, 
							    RemoteDebuggerServer debugger) {				
		this.file = file;
		this.debugger = debugger;
		this.threads = new IThread[0];
		this.launch = launch;
		if( launch!=null ) {
			launch.addDebugTarget( this );
		}		
		debugger.setTarget(this);
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		breakpointManager.addBreakpointListener(this);
		// we have to know when we get removed, so that we can shut off the debugger
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}
	
	public boolean canTerminate() {	
		return !isTerminated;
	}
	
	public boolean isTerminated() {
		return isTerminated;
	}

	public void terminate() throws DebugException {
        if(DEBUG){
            System.out.println( "TERMINATE" );
        }
        
		isTerminated = true;
		if (debugger != null){
			debugger.disconnect();
        }
        
		threads = new IThread[0];
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	public void setTerminated() {
		isTerminated = true;		
	}
	
	public void launchRemoved(ILaunch launch) {
		// shut down the remote debugger when parent launch
		if (launch == this.launch) {
			IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
			breakpointManager.removeBreakpointListener(this);
		}
	}

	public IProcess getProcess() {
		return null;
	}

	public ILaunch getLaunch() {
		return launch;
	}	
}
