package org.python.pydev.debug.newconsole.env;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.ui.console.IOConsole;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class defines a process that pydev will spawn
 */
public class PydevSpawnedInterpreterProcess implements IProcess {

    private boolean terminated;
    private IOConsole fConsole;
    private IStreamsProxy proxy;
	private Process spawnedInterpreterProcess;
	private ILaunch launch;
    
    public PydevSpawnedInterpreterProcess(Process spawnedInterpreterProcess, ILaunch launch){
		this.spawnedInterpreterProcess = spawnedInterpreterProcess;
		this.launch = launch;
    }

    /**
     * @return the console associated with the run
     */
    public IOConsole getIOConsole() {
        return fConsole;
    }
    
    public String getLabel() {
        return "Pydev Interactive Interpreter Process";
    }

    public ILaunch getLaunch() {
    	return this.launch;
    }

    public IStreamsProxy getStreamsProxy() {
        return null;
    }

    public void setAttribute(String key, String value) {
        throw new RuntimeException("not impl");
    }

    public String getAttribute(String key) {
        throw new RuntimeException("not impl");
    }

    public int getExitValue() throws DebugException {
        return 0;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public boolean canTerminate() {
        return true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() throws DebugException {
    	try {
    		if(this.spawnedInterpreterProcess != null){
    			this.spawnedInterpreterProcess.destroy();
    		}
		} catch (RuntimeException e) {
			PydevPlugin.log(e);
		}
		this.spawnedInterpreterProcess = null;
        terminated = true;
    }


}
