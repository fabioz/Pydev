package com.python.pydev.analysis.indexview;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.MisconfigurationException;

public abstract class ElementWithChildren extends ElementWithParent{

    public ElementWithChildren(ITreeElement parent) {
        super(parent);
    }
    
    private List<Object> calculatingChildren;
    private Object[] calculatedChildren;
    
    public Object[] getChildren() {
        if(calculatedChildren == null){
            calculatingChildren = new ArrayList<Object>();
            try {
                this.calculateChildren();
            } catch (MisconfigurationException e) {
                addChild(new MisconfigurationElement(this, e));
            }
            this.calculatedChildren = calculatingChildren.toArray(new ITreeElement[calculatingChildren.size()]);
            calculatingChildren = null;
        }
        return calculatedChildren;
    }

    protected abstract void calculateChildren() throws MisconfigurationException;
    
    protected void addChild(ITreeElement child) {
        this.calculatingChildren.add(child);
    }

}
