/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PyIndexContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return ((ITreeElement) inputElement).getChildren();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return ((ITreeElement) parentElement).getChildren();
    }

    @Override
    public Object getParent(Object element) {
        return ((ITreeElement) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        return ((ITreeElement) element).hasChildren();
    }

}
