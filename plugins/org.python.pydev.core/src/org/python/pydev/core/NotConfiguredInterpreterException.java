/*
 * License: Common Public License v1.0
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
        super("Interpreter not configured.\n" +
              "Go to window > preferences > PyDev > Python (or Jython) to configure it.");
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
