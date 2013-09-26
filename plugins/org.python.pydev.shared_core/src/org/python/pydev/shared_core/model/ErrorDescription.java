/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.model;

public class ErrorDescription {
    public String message;
    public int errorLine;
    public int errorStart;
    public int errorEnd;

    public ErrorDescription(String message, int errorLine, int errorStart, int errorEnd) {
        super();
        this.message = message;
        this.errorLine = errorLine;
        this.errorStart = errorStart;
        this.errorEnd = errorEnd;
    }
}
