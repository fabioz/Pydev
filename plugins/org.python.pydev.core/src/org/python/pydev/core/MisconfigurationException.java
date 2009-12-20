package org.python.pydev.core;

public class MisconfigurationException extends Exception{

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
