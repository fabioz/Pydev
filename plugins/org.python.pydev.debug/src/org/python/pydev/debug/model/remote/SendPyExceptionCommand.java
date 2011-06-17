/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;

public class SendPyExceptionCommand extends AbstractDebuggerCommand {

    public SendPyExceptionCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        String pyExceptions = instance.getExceptionsString();
        String breakOnUncaught = instance.getBreakOnUncaughtExceptions();
        String breakOnCaught = instance.getBreakOnCaughtExceptions();
        
        return makeCommand(AbstractDebuggerCommand.CMD_SET_PY_EXCEPTION, sequence, 
                StringUtils.join(ConfigureExceptionsFileUtils.DELIMITER, breakOnUncaught, breakOnCaught, pyExceptions));
    }
}
