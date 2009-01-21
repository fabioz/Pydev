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
        
        if(functionName != null && functionName.trim().length() > 0){
            cmd.append("\t**FUNC**").append(FullRepIterable.getLastPart(functionName));
        }
        
        cmd.append("\t").append(condition);
        
        return makeCommand(CMD_SET_BREAK, sequence, cmd.toString());
    }

}
