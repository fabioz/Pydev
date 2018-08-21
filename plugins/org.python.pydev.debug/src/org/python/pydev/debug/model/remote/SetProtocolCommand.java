/**
 * Copyright (c) 2018 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class SetProtocolCommand extends AbstractDebuggerCommand {

    public SetProtocolCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        return makeCommand(getCommandId(), sequence, "http");
    }

    @Override
    public boolean needResponse() {
        return false;
    }

    protected int getCommandId() {
        return CMD_SET_PROTOCOL;
    }

}
