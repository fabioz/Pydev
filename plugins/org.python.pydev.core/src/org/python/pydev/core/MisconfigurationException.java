/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

public class MisconfigurationException extends Exception {

    private static final long serialVersionUID = -1648414153963107493L;

    public MisconfigurationException() {

    }

    public MisconfigurationException(String msg) {
        super(msg);
    }

    public MisconfigurationException(Throwable cause) {
        super(cause);
    }

    public MisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
