package com.python.pydev.analysis.indexview;

import org.python.pydev.core.IInterpreterInfo;

public class InterpreterGroup extends ElementWithChildren {

    private IInterpreterInfo interpreterInfo;

    public InterpreterGroup(ITreeElement parent, IInterpreterInfo interpreterInfo) {
        super(parent);
        this.interpreterInfo = interpreterInfo;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }


    @Override
    protected void calculateChildren() {
        
        
    }

    
    @Override
    public String toString() {
        return this.interpreterInfo.getNameForUI();
    }


}
