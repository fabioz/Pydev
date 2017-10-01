package org.python.pydev.shared_core.io;

import java.io.UnsupportedEncodingException;

public class PyUnsupportedEncodingException extends UnsupportedEncodingException {

    private static final long serialVersionUID = 3821007258281155368L;

    private int line;
    private int character;

    public PyUnsupportedEncodingException(String encoding, int line, int character) {
        super(encoding);
        this.setLine(line);
        this.setCharacter(character);
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }
}
