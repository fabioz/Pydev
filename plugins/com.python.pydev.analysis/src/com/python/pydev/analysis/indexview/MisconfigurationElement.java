/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import org.python.pydev.core.MisconfigurationException;

public class MisconfigurationElement extends ElementWithChildren {

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
