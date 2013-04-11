package org.python.pydev.shared_core.string;

import java.util.Iterator;

/**
 * Class to help iterating through the document
 */
public class DocIterator implements Iterator<String> {
    private int startingLine;
    private boolean forward;
    private boolean isFirst = true;
    private int numberOfLines;
    private int lastReturnedLine = -1;
    private TextSelectionUtils ps;

    public DocIterator(boolean forward, TextSelectionUtils ps) {
        this(forward, ps, ps.getCursorLine(), true);
    }

    public DocIterator(boolean forward, TextSelectionUtils ps, int startingLine, boolean considerFirst) {
        this.startingLine = startingLine;
        this.forward = forward;
        numberOfLines = ps.getDoc().getNumberOfLines();
        this.ps = ps;
        if (!considerFirst) {
            isFirst = false;
        }
    }

    public int getCurrentLine() {
        return startingLine;
    }

    public boolean hasNext() {
        if (forward) {
            return startingLine < numberOfLines;
        } else {
            return startingLine >= 0;
        }
    }

    /**
     * Note that the first thing it returns is the lineContents to cursor (and only after that
     * does it return from the full line -- if it is iterating backwards).
     */
    public String next() {
        try {
            String line;
            if (forward) {
                line = ps.getLine(startingLine);
                lastReturnedLine = startingLine;
                startingLine++;
            } else {
                if (isFirst) {
                    line = ps.getLineContentsToCursor();
                    isFirst = false;
                } else {
                    line = ps.getLine(startingLine);
                }
                lastReturnedLine = startingLine;
                startingLine--;
            }
            return line;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remove() {
        throw new RuntimeException("Remove not implemented.");
    }

    public int getLastReturnedLine() {
        return lastReturnedLine;
    }
}