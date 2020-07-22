package org.python.pydev.core.docutils;

import java.util.Iterator;

public interface IPyDocIterator extends Iterator<String> {

    public int getLastReturnedLine();
}
