package org.python.pydev.editor;

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

