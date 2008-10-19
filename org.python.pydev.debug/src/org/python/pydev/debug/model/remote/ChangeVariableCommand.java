/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;


/**
 * ChangeVariable network command.
 * 
 * ChangeVariable gets the value of the variable from network as XML.
 */
public class ChangeVariableCommand extends AbstractDebuggerCommand {

    String locator;
    boolean isError = false;
    int responseCode;
    String payload;
    String expression;

    public ChangeVariableCommand(AbstractDebugTarget debugger, String locator, String expression) {
        super(debugger);
        this.locator = locator;
        this.expression = expression;
    }

    public String getOutgoing() {
        return makeCommand(getCommandId(), sequence, locator+"\t"+expression);
    }

    public boolean needResponse() {
        return false;
    }

    protected int getCommandId() {
        return CMD_CHANGE_VARIABLE;
    }
    
}

