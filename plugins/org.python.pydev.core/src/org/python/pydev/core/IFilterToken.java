package org.python.pydev.core;

public interface IFilterToken {
    boolean accept(String representation, int tokenType);
}
