/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 22, 2004
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

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_THREAD_RUN, sequence, thread);
    }
}
