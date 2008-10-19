package org.python.pydev.debug.model.remote;

import org.eclipse.debug.core.DebugException;
import org.python.pydev.debug.model.AbstractDebugTarget;

public abstract class AbstractRemoteDebugger {

    
    protected AbstractDebugTarget target = null;
    
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

    public AbstractDebugTarget getTarget() {
        return target;
    }

    public void setTarget(AbstractDebugTarget target) {
        this.target = target;
    }
    
}
