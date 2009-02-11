/*
 * Author: atotic
 * Created on May 6, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.core.FullRepIterable;
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
    public SetBreakpointCommand(AbstractDebugTarget debugger, String file, Object line, String condition, String functionName) {
        super(debugger);
        this.file = file;
        this.line = line;
        if (condition == null){
            this.condition = "None";
        }else{
            this.condition = condition;
        }
        this.functionName = functionName;
    }

    public String getOutgoing() {
        StringBuffer cmd = new StringBuffer().
        append(file).append("\t").append(line);
        
        if(functionName != null){
            cmd.append("\t**FUNC**").append(FullRepIterable.getLastPart(functionName).trim());
        }
        
        cmd.append("\t").append(condition);
        
        return makeCommand(CMD_SET_BREAK, sequence, cmd.toString());
    }

}
