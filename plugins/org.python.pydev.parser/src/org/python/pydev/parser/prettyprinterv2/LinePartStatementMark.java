package org.python.pydev.parser.prettyprinterv2;

public class LinePartStatementMark extends AbstractLinePart implements ILinePartStatementMark{

    private boolean isStart;

    public LinePartStatementMark(int beginCol, Object token, boolean isStart, PrettyPrinterDocLineEntry lineEntry) {
        super(beginCol, token, lineEntry);
        this.isStart = isStart;
    }
    
    public boolean isStart() {
        return isStart;
    }

    
    @Override
    public String toString() {
        return isStart?"START_STMT":"END_STMT";
    }
}

