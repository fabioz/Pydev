package org.python.pydev.parser.prettyprinterv2;


public class LinePartRequireMark extends AbstractLinePart implements ILinePart {

    public final String[] requireOneOf;


    public LinePartRequireMark(int beginColumn, String string, PrettyPrinterDocLineEntry lineEntry) {
        super(beginColumn, string, lineEntry);
        requireOneOf=null;
    }

    public LinePartRequireMark(int beginColumn, PrettyPrinterDocLineEntry lineEntry, String ... requireOneOf) {
        super(beginColumn, requireOneOf[0], lineEntry); //note that we pass the one at position 0 as the default.
        this.requireOneOf = requireOneOf;
    }

    public String getToken() {
        return (String) super.getToken();
    }
    
    public void setToken(String token) {
        this.token = token;
    }


    @Override
    public String toString() {
        return "RQ["+getToken()+"]";
    }
}
