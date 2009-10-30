package org.python.pydev.parser.prettyprinterv2;

public class LinePartIndentMark extends AbstractLinePart implements ILinePartIndentMark{

    private boolean isIndent;
    private boolean requireNewLine=false;

    public LinePartIndentMark(int beginCol, Object token, boolean isIndent, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.isIndent = isIndent;
    }

    
    @Override
    public String toString() {
        return isIndent?"INDENT":"DEDENT";
    }


    public void setRequireNewLine(boolean requireNewLine) {
        this.requireNewLine=requireNewLine;
    }
    
    public boolean getRequireNewLineOnIndent(){
        return this.requireNewLine;
    }


    public boolean isIndent() {
        return this.isIndent;
    }

}
