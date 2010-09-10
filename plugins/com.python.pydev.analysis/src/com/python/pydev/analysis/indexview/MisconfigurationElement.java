package com.python.pydev.analysis.indexview;

import org.python.pydev.core.MisconfigurationException;

public class MisconfigurationElement extends ElementWithChildren{

    private MisconfigurationException e;

    public MisconfigurationElement(ITreeElement parent, MisconfigurationException e) {
        super(parent);
        this.e = e;
    }

    @Override
    public String toString() {
        return e.toString();
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    protected void calculateChildren() {
    }
}
