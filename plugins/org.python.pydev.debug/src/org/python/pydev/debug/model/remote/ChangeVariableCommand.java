/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 30, 2004
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

    @Override
    public String getOutgoing() {
        return makeCommand(getCommandId(), sequence, locator + "\t" + expression);
    }

    @Override
    public boolean needResponse() {
        return false;
    }

    protected int getCommandId() {
        return CMD_CHANGE_VARIABLE;
    }

}
