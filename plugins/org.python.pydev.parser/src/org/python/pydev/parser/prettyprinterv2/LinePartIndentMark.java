package org.python.pydev.parser.prettyprinterv2;

public class LinePartIndentMark extends AbstractLinePart implements ILinePartIndentMark{

    private boolean isIndent;

    public LinePartIndentMark(int beginCol, Object token, boolean isIndent, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.isIndent = isIndent;
    }

    
    @Override
    public String toString() {
        return isIndent?"INDENT":"DEDENT";
    }
}
