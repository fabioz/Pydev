package com.python.pydev.analysis.indexview;

public class IndexRoot implements ITreeElement{

    Object[] children = null;
    
    public boolean hasChildren() {
        return true;
    }

    public Object[] getChildren() {
        if(children == null){
            children = new Object[]{new InterpretersGroup(this), new ProjectsGroup(this)};
        }
        return children;
    }

    public ITreeElement getParent() {
        return null; //the root has no parent
    }
    
    public String toString() {
        return "Index";
    }

}
