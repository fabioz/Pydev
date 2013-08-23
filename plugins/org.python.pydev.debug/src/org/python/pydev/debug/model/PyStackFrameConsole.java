/***************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com> - ongoing maintenance
***************************************************************************/
package org.python.pydev.debug.model;

import org.eclipse.debug.core.DebugException;

public class PyStackFrameConsole extends PyStackFrame {

    public PyStackFrameConsole(PyThread in_thread, AbstractDebugTarget target) {
        super(in_thread, "1", "frame_main", null, -1, target);
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
        return "__main__";
    }

    public int hashCode() {
        return getThread().hashCode();
    }

    public boolean equals(Object obj) {
        // All PyStackFrame Consoles look the same, so they are only equal if
        // they are identical
        return this == obj;
    }

}
