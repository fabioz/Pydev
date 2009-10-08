package org.python.pydev.parser.prettyprinterv2;

public class LinePartIndentMark extends AbstractLinePart implements ILinePartIndentMark{

    private boolean isIndent;
    private boolean requireNewLine=false;
    private int emptyLinesRequiredAfterDedent;

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


    @Override
    public boolean isIndent() {
        return this.isIndent;
    }


    public void setEmptyLinesRequiredAfterDedent(int emptyLinesRequiredAfterDedent) {
        this.emptyLinesRequiredAfterDedent = emptyLinesRequiredAfterDedent;
    }
}
