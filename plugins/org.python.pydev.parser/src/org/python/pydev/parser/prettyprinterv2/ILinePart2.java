package org.python.pydev.parser.prettyprinterv2;

public interface ILinePart2 extends ILinePart {

    public abstract int getLine();

    public abstract void setString(String string);

    public abstract String getString();

}