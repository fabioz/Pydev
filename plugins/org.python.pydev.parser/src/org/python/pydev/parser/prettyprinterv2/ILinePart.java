package org.python.pydev.parser.prettyprinterv2;

public interface ILinePart {

    public int getBeginCol();

    public abstract Object getToken();

    public int getLine();

    public int getLinePosition();

    public void setMarkAsFound();

    public boolean isMarkedAsFound();
}
