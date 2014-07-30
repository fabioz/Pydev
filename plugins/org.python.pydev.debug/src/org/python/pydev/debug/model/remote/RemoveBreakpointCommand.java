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
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Remove breakpoint command
 */
public class RemoveBreakpointCommand extends AbstractDebuggerCommand {

    public final String file;
    public final int breakpointId;
    public final String type;

    /**
     * @param type: django-line or python-line (PyBreakpoint.PY_BREAK_TYPE_XXX)
     */
    public RemoveBreakpointCommand(AbstractDebugTarget debugger, int breakpointId, String file, String type) {
        super(debugger);
        this.file = file;
        this.breakpointId = breakpointId;
        this.type = type;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_REMOVE_BREAK, sequence,
                StringUtils.join("\t", new String[] { type, file, Integer.toString(breakpointId) }));
    }
}
