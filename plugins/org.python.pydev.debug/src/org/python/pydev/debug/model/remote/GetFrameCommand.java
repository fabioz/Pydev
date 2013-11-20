/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

public class GetFrameCommand extends GetVariableCommand {

    public GetFrameCommand(AbstractDebugTarget debugger, String locator) {
        super(debugger, locator);
    }

    @Override
    protected int getCommandId() {
        return CMD_GET_FRAME;
    }

}
