/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * @author Hussain Bohra
 */
public class SetNextCommand extends AbstractDebuggerCommand {

    int commandId;
    String threadId;
    String funcName;
    int line;

    /**
     * @param command_id CMD_SET_NEXT_STATEMENT
     */
    public SetNextCommand(AbstractDebugTarget debugger, int command_id, String threadId, int line, String funcName) {
        super(debugger);
        this.commandId = command_id;
        this.threadId = threadId;
        this.line = line;
        this.funcName = funcName;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(commandId, sequence, threadId + "\t" + line + "\t" + funcName);
    }

}
