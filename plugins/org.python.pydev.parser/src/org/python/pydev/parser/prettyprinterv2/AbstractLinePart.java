package org.python.pydev.parser.prettyprinterv2;

public abstract class AbstractLinePart implements ILinePart{
    
    protected Object token;
    private final int beginCol;
    private PrettyPrinterDocLineEntry lineEntry;
    
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
}
