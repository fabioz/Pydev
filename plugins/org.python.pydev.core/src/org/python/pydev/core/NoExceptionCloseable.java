package org.python.pydev.core;

public interface NoExceptionCloseable extends AutoCloseable {

    @Override
    public void close();
}
