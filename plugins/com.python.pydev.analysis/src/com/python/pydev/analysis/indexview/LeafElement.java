package com.python.pydev.analysis.indexview;

public class LeafElement extends ElementWithParent {

    private Object[] EMPTY = new Object[0];
    
    private Object o;

    public LeafElement(ITreeElement parent, Object o) {
        super(parent);
        this.o = o;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public Object[] getChildren() {
        return EMPTY;
    }
    
    @Override
    public String toString() {
        return o.toString();
    }

}
