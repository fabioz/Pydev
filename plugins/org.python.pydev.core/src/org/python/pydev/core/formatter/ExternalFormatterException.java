/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.formatter;

/**
 * An external formatter (black, ruff, ...) could not be run or did not succeed: it was not found,
 * exited with an error (e.g. an option it does not support, a syntax error in the code) or timed out.
 *
 * The message carries what's needed to diagnose it (command line, stdout/stderr of the tool).
 */
public class ExternalFormatterException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String toolName;

    public ExternalFormatterException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
    }

    public ExternalFormatterException(String toolName, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
    }

    /**
     * @return the user-facing name of the tool (e.g. "black", "ruff").
     */
    public String getToolName() {
        return toolName;
    }
}
