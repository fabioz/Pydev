package org.python.pydev.parser.prettyprinterv2;

public class LinePartRequireIndentMark extends LinePartRequireMark{


    public LinePartRequireIndentMark(int beginColumn, String string, PrettyPrinterDocLineEntry prettyPrinterDocLineEntry) {
        super(beginColumn, string, prettyPrinterDocLineEntry);
    }
    
    @Override
    public String toString() {
        return "INDENT";
    }

}
