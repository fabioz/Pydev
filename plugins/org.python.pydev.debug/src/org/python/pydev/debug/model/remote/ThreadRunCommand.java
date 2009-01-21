/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Run thread network command
 */
public class ThreadRunCommand extends AbstractDebuggerCommand {

    String thread;
    
    public ThreadRunCommand(AbstractDebugTarget debugger, String thread) {
        super(debugger);
        this.thread = thread;
    }

    public String getOutgoing() {
        return makeCommand(CMD_THREAD_RUN, sequence, thread);
    }
}
