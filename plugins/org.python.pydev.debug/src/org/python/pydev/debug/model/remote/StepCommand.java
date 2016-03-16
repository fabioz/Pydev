/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 27, 2004
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Debugger step command.
 */
public class StepCommand extends AbstractDebuggerCommand {

    int commandId;
    String threadId;

    /**
     * 
     * @param commandId CMD_STEP_INTO CMD_STEP_OVER CMD_STEP_RETURN
     */
    public StepCommand(AbstractDebugTarget debugger, int commandId, String threadId) {
        super(debugger);
        this.commandId = commandId;
        this.threadId = threadId;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(commandId, sequence, threadId);
    }

}
