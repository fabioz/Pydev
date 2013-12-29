/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.log;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Tuple;

public class Log {

    private static final Map<Tuple<Integer, String>, Long> lastLoggedTime = new HashMap<Tuple<Integer, String>, Long>();
    /**
     * Only applicable when SharedCorePlugin.inTestMode() == true
     */
    private static final int DEBUG_LEVEL = IStatus.WARNING;

    public static CoreException log(Throwable e) {
        return log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten (null message).", e);
    }

    public static CoreException log(String msg) {
        return log(IStatus.ERROR, msg, new RuntimeException(msg));
    }

    public static CoreException log(String msg, Throwable e) {
        return log(IStatus.ERROR, msg, e);
    }

    public static CoreException logWarning(String msg) {
        return log(IStatus.WARNING, msg, new RuntimeException(msg));
    }

    public static CoreException logInfo(Throwable e) {
        return log(IStatus.INFO, e.getMessage(), e);
    }

    public static CoreException logInfo(String msg) {
        return log(IStatus.INFO, msg, new RuntimeException(msg));
    }

    public static CoreException logInfo(String msg, Throwable e) {
        return log(IStatus.INFO, msg, e);
    }

    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     * @return CoreException that can be thrown for the given log event
     */
    public static CoreException log(int errorLevel, String message, Throwable e) {
        final String id;
        if (SharedCorePlugin.inTestMode()) {
            id = "SharedCorePlugin";
        } else {
            SharedCorePlugin plugin = SharedCorePlugin.getDefault();
            id = plugin.getBundle().getSymbolicName();
        }

        Status s = new Status(errorLevel, id, errorLevel, message, e);
        CoreException coreException = new CoreException(s);

        Tuple<Integer, String> key = new Tuple<Integer, String>(errorLevel, message);
        synchronized (lastLoggedTime) {
            Long lastLoggedMillis = lastLoggedTime.get(key);
            long currentTimeMillis = System.currentTimeMillis();
            if (lastLoggedMillis != null) {
                if (currentTimeMillis < lastLoggedMillis + (20 * 1000)) {
                    //System.err.println("Skipped report of:"+message);
                    return coreException; //Logged in the last 20 seconds, so, just skip it for now
                }
            }
            lastLoggedTime.put(key, currentTimeMillis);
        }
        try {
            if (SharedCorePlugin.inTestMode()) {
                if (DEBUG_LEVEL <= errorLevel) {
                    System.err.println(message);
                    if (e != null) {
                        e.printStackTrace();
                    }
                }
            } else {
                SharedCorePlugin plugin = SharedCorePlugin.getDefault();
                plugin.getLog().log(s);
            }
        } catch (Exception e1) {
            //logging should not fail!
        }
        return coreException;
    }
}
