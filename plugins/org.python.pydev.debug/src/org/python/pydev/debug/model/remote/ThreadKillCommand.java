/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * KILL_THREAD debugger command
 * 
 */
public class ThreadKillCommand extends AbstractDebuggerCommand {

    String thread_id;
    
    public ThreadKillCommand(AbstractDebugTarget debugger, String thread_id) {
        super(debugger);
        this.thread_id = thread_id;    
    }
    
    public String getOutgoing() {
        return makeCommand(CMD_THREAD_KILL, sequence, thread_id);
    }

}
