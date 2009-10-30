package org.python.pydev.parser.prettyprinterv2;

/**
 * Base class for line parts.
 */
public abstract class AbstractLinePart implements ILinePart{
    
    protected Object token;
    private final int beginCol;
    private PrettyPrinterDocLineEntry lineEntry;
    private boolean found;
    
    public AbstractLinePart(int beginCol, Object token, PrettyPrinterDocLineEntry lineEntry) {
        this.beginCol = beginCol;
        this.token = token;
        this.lineEntry = lineEntry;
    }


    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getToken()
     */
    public Object getToken() {
        return token;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getBeginCol()
     */
    public int getBeginCol() {
        return beginCol;
    }
    
    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getLine()
     */
    public int getLine() {
        return this.lineEntry.line;
    }
    
    public int getLinePosition() {
        return this.lineEntry.getSortedParts().indexOf(this);
    }
    
    public void setMarkAsFound() {
        this.found = true;
    }
    
    public boolean isMarkedAsFound() {
        return found;
    }
}
