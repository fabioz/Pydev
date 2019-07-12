/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

public class IndexRoot implements ITreeElement {

    Object[] children = null;

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public Object[] getChildren() {
        if (children == null) {
            children = new Object[] { new InterpretersGroup(this), new ProjectsGroup(this) };
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
