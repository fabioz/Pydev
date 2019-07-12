/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.log;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * @author Fabio
 */
public class Log {

    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     * @return CoreException that can be thrown for the given log event
     */
    public static CoreException log(int errorLevel, String message, Throwable e) {
        return org.python.pydev.shared_core.log.Log.log(errorLevel, message, e);
    }

    public static CoreException log(Throwable e) {
        return log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten (null message).", e);
    }

    public static CoreException log(String msg) {
        return log(IStatus.ERROR, msg, new RuntimeException(msg));
    }

    public static CoreException log(String msg, Throwable e) {
        return log(IStatus.ERROR, msg, e);
    }

    public static CoreException logInfo(Throwable e) {
        return log(IStatus.INFO, e.getMessage(), e);
    }

    public static CoreException logInfo(String msg) {
        return log(IStatus.INFO, msg, new RuntimeException(msg));
    }

    public static CoreException logWarn(String msg) {
        return log(IStatus.WARNING, msg, new RuntimeException(msg));
    }

    public static CoreException logInfo(String msg, Throwable e) {
        return log(IStatus.INFO, msg, e);
    }

}
