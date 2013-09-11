/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 3, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

/**
 * @author Fabio Zadrozny
 */
public class NotConfiguredInterpreterException extends MisconfigurationException {

    private static final long serialVersionUID = -7824508734113060512L;

    public NotConfiguredInterpreterException() {
        super("Interpreter not configured.\n"
                + "Go to window > preferences > PyDev > Python (or Jython) to configure it.");
    }

    /**
     * @param message
     */
    public NotConfiguredInterpreterException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public NotConfiguredInterpreterException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public NotConfiguredInterpreterException(String message, Throwable cause) {
        super(message, cause);
    }

}
