/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
