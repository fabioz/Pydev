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

/**
 * This class represents a stack frame for the "virtual" frame of the 
 * interactive console. When no code is running, there is no real frame
 * that this represents, but rather it represents the set of Globals
 * that are used by execLine() to run the user's typed code in.
 * <p>
 * This frame lives as the one frame in {@link PyThreadConsole}.
 */
public class PyStackFrameConsole extends PyStackFrame {
    public static final String VIRTUAL_FRAME_ID = "1";

    public PyStackFrameConsole(PyThread in_thread, AbstractDebugTarget target) {
        super(in_thread, VIRTUAL_FRAME_ID, "frame_main", null, -1, target);
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
        // This matches hard coded __main__ in pydevconsole.py
        return "__main__ [<console>:0]";
    }

    @Override
    public int hashCode() {
        return getThread().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // All PyStackFrame Consoles look the same, so they are only equal if
        // they are identical
        return this == obj;
    }

}
