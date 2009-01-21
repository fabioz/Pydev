/*
 * Author: atotic
 * Created on Apr 19, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Version debugger command.
 * 
 * See protocol definition for more info. Used as
 */
public class VersionCommand extends AbstractDebuggerCommand {

    static final String VERSION = "1.0";
    
    /**
     * @param debugger
     */
    public VersionCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    public String getOutgoing() {
        return makeCommand(CMD_VERSION, sequence, VERSION);
    }

    public boolean needResponse() {
        return true;
    }
    
    public void processOKResponse(int cmdCode, String payload) {
//        System.err.println("The version is " + payload);
// not checking for versioning in 1.0, might come in useful later
    }

}
