/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

public abstract class ElementWithParent implements ITreeElement {

    protected ITreeElement parent;

    public ElementWithParent(ITreeElement parent) {
        this.parent = parent;
    }

    @Override
    public ITreeElement getParent() {
        return this.parent;
    }
}
