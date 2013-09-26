/******************************************************************************
* Copyright (C) 2011-2012  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyPropertyTraceManager;

public class SetPropertyTraceCommand extends AbstractDebuggerCommand {

    public SetPropertyTraceCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    @Override
    public String getOutgoing() {
        PyPropertyTraceManager instance = PyPropertyTraceManager.getInstance();
        String pyPropertyTraceState = instance.getPyPropertyTraceState().trim();
        return makeCommand(AbstractDebuggerCommand.CMD_SET_PROPERTY_TRACE, sequence, pyPropertyTraceState);
    }

}
