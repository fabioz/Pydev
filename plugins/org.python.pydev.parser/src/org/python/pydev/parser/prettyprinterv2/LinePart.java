package org.python.pydev.parser.prettyprinterv2;

public class LinePart extends AbstractLinePart implements ILinePart, ILinePart2 {

    private String string;

    public LinePart(int beginCol, String string, Object token, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.setString(string);
    }



    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#setString(java.lang.String)
     */
    public void setString(String string) {
        this.string = string;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.parser.prettyprinterv2.ILinePart2#getString()
     */
    public String getString() {
        return string;
    }


}
