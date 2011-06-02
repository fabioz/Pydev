/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.env;

import java.util.HashMap;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.ui.console.IOConsole;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;

/**
 * This class defines a process that pydev will spawn for the console.
 */
public class PydevSpawnedInterpreterProcess implements IProcess {

    /**
     * Boolean determining if this process was already terminated or not.
     */
    private boolean terminated;
    
    private Process spawnedInterpreterProcess;
    private ILaunch launch;
    private HashMap<String, String> attributes;
    
    public PydevSpawnedInterpreterProcess(Process spawnedInterpreterProcess, ILaunch launch){
        this.spawnedInterpreterProcess = spawnedInterpreterProcess;
        this.launch = launch;
        this.attributes = new HashMap<String, String>();
        this.setAttribute(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
    }

    /**
     * @return the console associated with the run (null in this case)
     */
    public IOConsole getIOConsole() {
        return null;
    }
    
    public String getLabel() {
        return "PyDev Interactive Interpreter Process";
    }

    public ILaunch getLaunch() {
        return this.launch;
    }

    public IStreamsProxy getStreamsProxy() {
        return null;
    }

    public void setAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return this.attributes.get(key);
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
            Log.log(e);
        }
        this.spawnedInterpreterProcess = null;
        terminated = true;
    }


}
