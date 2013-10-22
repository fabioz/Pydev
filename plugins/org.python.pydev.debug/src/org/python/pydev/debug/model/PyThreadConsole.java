/******************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IProcess;

/**
 * A specialisation of PyThread that can't be "controlled" by the user.
 * <p>
 * We use this thread to represent the virtual thread/frame that is the 
 * one of the interactive console. See {@link PyStackFrameConsole}.
 * <p>
 * This thread is prepended to the list of real frames returned from 
 * pydevd in {@link PyDebugTargetConsole#getThreads()}
 */
public class PyThreadConsole extends PyThread {

    public static final String VIRTUAL_CONSOLE_ID = "console_main";

    public PyThreadConsole(AbstractDebugTarget target) {
        super(target, VIRTUAL_CONSOLE_ID, VIRTUAL_CONSOLE_ID);
    }

    @Override
    public boolean canResume() {
        return false;
    }

    @Override
    public boolean canStepInto() {
        return false;
    }

    @Override
    public boolean canStepOver() {
        return false;
    }

    @Override
    public boolean canStepReturn() {
        return false;
    }

    @Override
    public boolean canSuspend() {
        return false;
    }

    @Override
    public String getName() throws DebugException {
        if (getDebugTarget() == null || getDebugTarget().getProcess() == null) {
            // probably being terminated, return constant string
            return "Interactive Console";
        }
        IProcess process = getDebugTarget().getProcess();
        return "Interactive Console: " + process.getLabel();
    }
}
