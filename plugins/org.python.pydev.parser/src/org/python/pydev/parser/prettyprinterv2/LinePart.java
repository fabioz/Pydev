package org.python.pydev.parser.prettyprinterv2;

public class LinePart {

    public String string;
    public final Object token;
    public final int beginCol;

    public LinePart(int beginCol, String string, Object token) {
        this.beginCol = beginCol;
        this.string = string;
        this.token = token;
    }

}
