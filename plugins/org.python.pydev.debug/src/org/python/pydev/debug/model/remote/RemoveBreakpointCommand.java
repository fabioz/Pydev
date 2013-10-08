/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 6, 2004
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Remove breakpoint command
 */
public class RemoveBreakpointCommand extends AbstractDebuggerCommand {

    public String file;
    public Object line;

    public RemoveBreakpointCommand(AbstractDebugTarget debugger, String file, Object line) {
        super(debugger);
        this.file = file;
        this.line = line;
    }

    public String getOutgoing() {
        return makeCommand(CMD_REMOVE_BREAK, sequence, file + "\t" + line.toString());
    }
}
