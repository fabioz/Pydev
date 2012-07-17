/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Set breakpoint command.
 */
public class SetBreakpointCommand extends AbstractDebuggerCommand {

    public String file;
    public Object line;
    public String condition;
    private String functionName;

    /**
     * @param functionName 
     * - If functionName == "None" or null it'll match any context (so, any statement in the file will be debugged). 
     * - If functionName == "", it'll match only statements in the global level (not inside functions)
     * - If functionName == "The name of some function", it'll only debug statements inside a function with the same name. 
     */
    public SetBreakpointCommand(AbstractDebugTarget debugger, String file, Object line, String condition,
            String functionName) {
        super(debugger);
        this.file = file;
        this.line = line;
        if (condition == null) {
            this.condition = "None";
        } else {
            this.condition = condition;
        }
        this.functionName = functionName;
    }

    public String getOutgoing() {
        FastStringBuffer cmd = new FastStringBuffer().append(file).append("\t").appendObject(line);

        if (functionName != null) {
            cmd.append("\t**FUNC**").append(FullRepIterable.getLastPart(functionName).trim());
        }

        cmd.append("\t").append(condition);

        return makeCommand(CMD_SET_BREAK, sequence, cmd.toString());
    }

}
