package org.python.pydev.shared_core.io;

import java.io.UnsupportedEncodingException;

public class PyUnsupportedEncodingException extends UnsupportedEncodingException {

    private static final long serialVersionUID = 3821007258281155368L;

    private final int line;
    private final int column;

    public PyUnsupportedEncodingException(String encoding, int line, int column) {
        super(encoding);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

}
