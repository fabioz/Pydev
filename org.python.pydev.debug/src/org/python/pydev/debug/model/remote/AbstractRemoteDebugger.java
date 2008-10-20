package org.python.pydev.debug.model.remote;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.python.pydev.debug.model.AbstractDebugTarget;

public abstract class AbstractRemoteDebugger {

    
    protected List<AbstractDebugTarget> targets = new ArrayList<AbstractDebugTarget>();
    
    /**
     * debugger should finish when this is called
     */
    public abstract void dispose();
    
    /**
     * debugger is disconnected when this is called
     * 
     * @throws DebugException
     */
    public abstract void disconnect() throws DebugException;

    public void addTarget(AbstractDebugTarget pyDebugTarget) {
        this.targets.add(pyDebugTarget);
    }    

    
}
