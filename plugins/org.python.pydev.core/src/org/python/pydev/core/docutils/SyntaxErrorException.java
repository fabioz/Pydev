package org.python.pydev.core.docutils;

public class SyntaxErrorException extends Exception{

    private static final long serialVersionUID = -2833305218650293506L;

    public SyntaxErrorException(){
        super("Syntax error in buffer.");
    }
}
