package org.python.pydev.parser.prettyprinterv2;

/**
 * Defines a line part that has an associated string.
 */
public interface ILinePart2 extends ILinePart {

    public abstract void setString(String string);

    public abstract String getString();

}