/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.shared_core.string.StringUtils;

public class SendPyExceptionCommand extends AbstractDebuggerCommand {

    public SendPyExceptionCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        String pyExceptions = instance.getExceptionsString().trim();
        boolean breakOnUncaught = instance.getBreakOnUncaughtExceptions();
        boolean breakOnCaught = instance.getBreakOnCaughtExceptions();
        boolean skipCaughtExceptionsInSameFunction = instance.getSkipCaughtExceptionsInSameFunction();
        boolean ignoreExceptionsThrownInLinesWithIgnoreException = instance
                .getIgnoreExceptionsThrownInLinesWithIgnoreException();

        return makeCommand(AbstractDebuggerCommand.CMD_SET_PY_EXCEPTION, sequence,
                StringUtils.join(ConfigureExceptionsFileUtils.DELIMITER,
                        breakOnUncaught, breakOnCaught, skipCaughtExceptionsInSameFunction,
                        ignoreExceptionsThrownInLinesWithIgnoreException, pyExceptions));
    }
}
