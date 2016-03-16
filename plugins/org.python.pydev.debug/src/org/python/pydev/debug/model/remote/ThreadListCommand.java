/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 21, 2004
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyThread;
import org.python.pydev.debug.model.XMLUtils;


/**
 * ListThreads command.
 * 
 * See protocol for more info
 */
public class ThreadListCommand extends AbstractDebuggerCommand {

    boolean done;
    PyThread[] threads;

    public ThreadListCommand(AbstractDebugTarget target) {
        super(target);
        done = false;
    }

    public void waitUntilDone(int timeout) throws InterruptedException {
        while (!done && timeout > 0) {
            timeout -= 100;
            synchronized (this) {
                Thread.sleep(100);
            }
        }
        if (timeout < 0)
            throw new InterruptedException();
    }

    public PyThread[] getThreads() {
        return threads;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_LIST_THREADS, sequence, "");
    }

    @Override
    public boolean needResponse() {
        return true;
    }

    /**
     * The response is a list of threads
     */
    @Override
    public void processOKResponse(int cmdCode, String payload) {
        if (cmdCode != 102) {
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected response to LIST THREADS" + payload, null);
            return;
        }
        try {
            threads = XMLUtils.ThreadsFromXML(target, payload);
        } catch (CoreException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an unexpected response " + payload, null);
            Log.log(e);
        }
        done = true;
    }

    @Override
    public void processErrorResponse(int cmdCode, String payload) {
        PydevDebugPlugin.log(IStatus.ERROR, "LIST THREADS got an error " + payload, null);
    }
}
