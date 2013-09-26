/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.python.pydev.debug.model.AbstractDebugTarget;

public abstract class AbstractRemoteDebugger {

    protected List<AbstractDebugTarget> targets = new ArrayList<AbstractDebugTarget>();

    /**
     * debugger should finish when this is called
     */
    public abstract void dispose();

    /**
     * debugger is disconnected when this is called
     * 
     * @throws DebugException
     */
    public abstract void disconnect() throws DebugException;

    public void addTarget(AbstractDebugTarget pyDebugTarget) {
        this.targets.add(pyDebugTarget);
    }

}
