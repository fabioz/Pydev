/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 19, 2004
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.utils.PlatformUtils;

/**
 * Version debugger command.
 *
 * See protocol definition for more info. Used as
 */
public class VersionCommand extends AbstractDebuggerCommand {

    static final String VERSION = "1.1";

    /**
     * @param debugger
     */
    public VersionCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        return makeCommand(CMD_VERSION, sequence,
                VERSION +
                        "\t" + (PlatformUtils.isWindowsPlatform() ? "WINDOWS" : "UNIX") +
                        "\tID");
    }

    @Override
    public boolean needResponse() {
        return true;
    }

    @Override
    public void processOKResponse(int cmdCode, String payload) {
        //        System.err.println("The version is " + payload);
        // not checking for versioning in 1.0, might come in useful later
    }

}
