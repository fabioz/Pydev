/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.util.List;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;

public class SendPyExceptionCommand extends AbstractDebuggerCommand {

    public SendPyExceptionCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        List<String> pyExceptions = instance.getExceptionsList();
        boolean breakOnUncaught = instance.getBreakOnUncaughtExceptions();
        boolean breakOnUserUncaught = instance.getBreakOnUserUncaughtExceptions();
        boolean breakOnCaught = instance.getBreakOnCaughtExceptions();
        boolean skipCaughtExceptionsInSameFunction = instance.getSkipCaughtExceptionsInSameFunction();
        boolean skipCaughtExceptionsInLibraries = instance.getSkipCaughtExceptionsInLibraries();
        boolean ignoreExceptionsThrownInLinesWithIgnoreException = instance
                .getIgnoreExceptionsThrownInLinesWithIgnoreException();

        JsonObject root = new JsonObject();
        root.add("break_on_uncaught", breakOnUncaught);
        root.add("break_on_user_caught", breakOnUserUncaught);
        root.add("break_on_caught", breakOnCaught);
        root.add("skip_on_exceptions_thrown_in_same_context", skipCaughtExceptionsInSameFunction);
        root.add("ignore_exceptions_thrown_in_lines_with_ignore_exception",
                ignoreExceptionsThrownInLinesWithIgnoreException);
        root.add("ignore_libraries", skipCaughtExceptionsInLibraries);

        JsonArray exceptionTypes = new JsonArray();
        for (String s : pyExceptions) {
            exceptionTypes.add(s);
        }
        root.add("exception_types", exceptionTypes);

        return makeCommand(AbstractDebuggerCommand.CMD_SET_PY_EXCEPTION_JSON, sequence, root.toString());
    }
}
