package com.python.pydev.analysis.indexview;

public interface ITreeElement {

    boolean hasChildren();

    Object[] getChildren();

    ITreeElement getParent();
    
}
