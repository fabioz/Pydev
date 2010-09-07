package com.python.pydev.analysis.indexview;

public class IndexRoot implements ITreeElement{

    Object[] children = null;
    
    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public Object[] getChildren() {
        if(children == null){
            children = new Object[]{new InterpretersGroup(this), new ProjectsGroup(this)};
        }
        return children;
    }

    @Override
    public ITreeElement getParent() {
        return null; //the root has no parent
    }
    
    @Override
    public String toString() {
        return "Index";
    }

}
