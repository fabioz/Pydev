package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;

public interface IPyDocIterator extends Iterator<String> {

    public void setStartingOffset(int offset);

    public int getLastReturnedLine();

    void setStartingLine(int line) throws BadLocationException;
}
