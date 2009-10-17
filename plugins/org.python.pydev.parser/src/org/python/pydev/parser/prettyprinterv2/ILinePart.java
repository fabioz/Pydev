package org.python.pydev.parser.prettyprinterv2;

/**
 * Defines a part of a line of the document we will build while making the pretty-printing.
 */
public interface ILinePart {

    public int getBeginCol();

    public abstract Object getToken();

    public int getLine();

    public int getLinePosition();

    public void setMarkAsFound();

    public boolean isMarkedAsFound();
}
