/*
 * Author: atotic
 * Created on May 4, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

/**
 * All commands are executed asynchronously.
 * 
 * This interface, if specified, is called when command completes.
 */
public interface ICommandResponseListener {
    public void commandComplete(AbstractDebuggerCommand cmd);
}
