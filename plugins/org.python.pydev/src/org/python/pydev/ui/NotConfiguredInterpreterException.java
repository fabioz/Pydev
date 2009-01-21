/*
 * License: Common Public License v1.0
 * Created on Jun 3, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

/**
 * @author Fabio Zadrozny
 */
public class NotConfiguredInterpreterException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -7824508734113060512L;

    /**
     * 
     */
    public NotConfiguredInterpreterException() {
        super("Interpreter is not properly configured!\n" +
              "Please go to window->preferences->PyDev->Python (or Jython) Interpreters and configure it.");
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
