package org.python.pydev.parser.jython;

public final class SpecialStr implements ISpecialStr{
    
    public final String str;
    public final int beginLine;
    public final int beginCol;

    public SpecialStr(String str, int beginLine, int beginCol){
        this.str = str;
        this.beginLine = beginLine;
        this.beginCol = beginCol;
    }
    
    @Override
    public String toString() {
        return str;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SpecialStr)){
            return false;
        }
        return str.equals(((SpecialStr)obj).str);
    }
    
    public int getBeginCol() {
        return beginCol;
    }
    
    public int getBeginLine() {
        return beginLine;
    }
}
