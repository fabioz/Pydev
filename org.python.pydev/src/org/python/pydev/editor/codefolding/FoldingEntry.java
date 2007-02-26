/**
 * 
 */
package org.python.pydev.editor.codefolding;

import org.python.pydev.parser.visitors.scope.ASTEntry;

public class FoldingEntry{

    public FoldingEntry(int type, int startLine, int endLine, ASTEntry astEntry) {
        if(endLine < startLine){
            endLine = startLine;
        }
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
        this.astEntry = astEntry;
    }
    public final static int TYPE_IMPORT=1;
    public final static int TYPE_DEF=2;
    public final static int TYPE_COMMENT=3;
    public final static int TYPE_STR=4;
    public int type;
    public int startLine;
    public int endLine;
    public ASTEntry astEntry;
    
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + endLine;
        result = PRIME * result + startLine;
        result = PRIME * result + type;
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FoldingEntry other = (FoldingEntry) obj;
        if (endLine != other.endLine)
            return false;
        if (startLine != other.startLine)
            return false;
        if (type != other.type)
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("FoldingEntry<");
        buf.append("type:");
        buf.append(type);
        buf.append(" startLine:");
        buf.append(startLine);
        buf.append(" endLine:");
        buf.append(endLine);
        buf.append(">");
        return buf.toString();
    }

    public ASTEntry getAstEntry() {
        return astEntry;
    }
}