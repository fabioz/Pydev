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
import org.eclipse.debug.core.model.IProcess;

/**
 * A specialisation of PyThread that can't be "controlled" by the user.
 */
public class PyThreadConsole extends PyThread {

    public PyThreadConsole(AbstractDebugTarget target) {
        super(target, "console_main", "console_main");
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
        IProcess process = getDebugTarget().getProcess();
        return process.getLabel();
    }
}
