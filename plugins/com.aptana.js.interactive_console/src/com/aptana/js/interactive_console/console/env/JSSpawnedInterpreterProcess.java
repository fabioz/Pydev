/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console.env;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;

/**
 * This class defines a process that we'll spawn for the console.
 */
public class JSSpawnedInterpreterProcess extends RuntimeProcess {

    public JSSpawnedInterpreterProcess(ILaunch launch, Process process, String name, Map attributes) {
        super(launch, process, name, attributes);
        this.setAttribute(IProcess.ATTR_PROCESS_TYPE, "JAVASCRIPT.PROCESS");
    }

    /**
     * JSSpawnedInterpreterProcess handles the IO in a custom way, so we don't 
     * use the streams proxy.
     */
    @Override
    protected IStreamsProxy createStreamsProxy() {
        // do nothing
        return null;
    }
}
