/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.MisconfigurationException;

public abstract class ElementWithChildren extends ElementWithParent {

    public ElementWithChildren(ITreeElement parent) {
        super(parent);
    }

    private List<Object> calculatingChildren;
    private Object[] calculatedChildren;

    @Override
    public Object[] getChildren() {
        if (calculatedChildren == null) {
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
