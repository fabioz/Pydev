package org.python.pydev.parser.prettyprinterv2;

public class LinePart {

    public String string;
    public final Object token;
    public final int beginCol;
    private PrettyPrinterDocLineEntry lineEntry;

    public LinePart(int beginCol, String string, Object token, PrettyPrinterDocLineEntry lineEntry) {
        this.beginCol = beginCol;
        this.string = string;
        this.token = token;
        this.lineEntry = lineEntry;
    }

    public int getLine() {
        return this.lineEntry.line;
    }

}
