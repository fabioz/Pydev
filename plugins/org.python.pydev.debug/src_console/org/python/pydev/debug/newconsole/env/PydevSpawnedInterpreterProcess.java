/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.env;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.python.pydev.debug.core.Constants;

/**
 * This class defines a process that pydev will spawn for the console.
 */
public class PydevSpawnedInterpreterProcess extends RuntimeProcess {

    public PydevSpawnedInterpreterProcess(ILaunch launch, Process process, String name, String encoding) {
        super(launch, process, name, null);
        this.setAttribute(IProcess.ATTR_PROCESS_TYPE, Constants.PROCESS_TYPE);
        this.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, encoding);
    }

    /**
     * PydevSpawnedInterpreterProcess handles the IO in a custom way, so we don't
     * use the streams proxy.
     */
    @Override
    protected IStreamsProxy createStreamsProxy() {
        // do nothing
        return null;
    }
}
