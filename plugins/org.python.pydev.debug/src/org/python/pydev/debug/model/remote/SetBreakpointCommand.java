/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 6, 2004
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Set breakpoint command.
 */
public class SetBreakpointCommand extends AbstractDebuggerCommand {

    public final String file;
    public final Object line;
    public final String condition;
    private final String functionName;
    private final int breakpointId;
    private final String type;

    /**
     * @param functionName
     * - If functionName == "None" or null it'll match any context (so, any statement in the file will be debugged).
     * - If functionName == "", it'll match only statements in the global level (not inside functions)
     * - If functionName == "The name of some function", it'll only debug statements inside a function with the same name.
     *
     * @param type: django-line or python-line (PyBreakpoint.PY_BREAK_TYPE_XXX)
     */
    public SetBreakpointCommand(AbstractDebugTarget debugger, int breakpointId, String file, Object line,
            String condition, String functionName, String type) {
        super(debugger);
        this.file = file;
        this.line = line;
        if (condition == null) {
            this.condition = "None";
        } else {
            this.condition = condition;
        }
        this.functionName = functionName;
        this.breakpointId = breakpointId;
        this.type = type;
    }

    @Override
    public String getOutgoing() {
        if (file == null || line == null) {
            return null;
        }
        FastStringBuffer cmd = new FastStringBuffer().
                append(this.breakpointId).
                append('\t').append(type).
                append('\t').append(file).
                append('\t').appendObject(line);

        if (functionName != null) {
            cmd.append("\t").append(FullRepIterable.getLastPart(functionName).trim());
        } else {
            cmd.append("\tNone");
        }

        cmd.append('\t').append(condition);

        String expression = "None";
        cmd.append('\t').append(expression);

        return makeCommand(CMD_SET_BREAK, sequence, cmd.toString());
    }

}
