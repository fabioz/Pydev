/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Debugger reload command.
 */
public class ReloadCodeCommand extends AbstractDebuggerCommand {

    private String moduleName;

    public ReloadCodeCommand(AbstractDebugTarget debugger, String moduleName) {
        super(debugger);
        this.moduleName = moduleName;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_RELOAD_CODE, sequence, this.moduleName);
    }

}
