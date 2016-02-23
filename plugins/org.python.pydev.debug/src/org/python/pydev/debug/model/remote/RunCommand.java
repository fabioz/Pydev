/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 7, 2004
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Run command
 */
public class RunCommand extends AbstractDebuggerCommand {

    public RunCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_RUN, sequence, "");
    }
}
