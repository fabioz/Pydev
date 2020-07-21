/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.quick_outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_ui.dialogs.ISizableContentProvider;

public class DataAndImageTreeNodeContentProvider implements ITreeContentProvider, ISizableContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement == null) {
            return new Object[0];
        }
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) parentElement;
        return m.childrenAsArray();
    }

    @Override
    public Object getParent(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.hasChildren();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isBig(Object input) {
        final Counter counter = new Counter();
        if (input == null) {
            return false;
        }
        // We consider a tree with a size of 400 items to be big.
        int SIZE_TO_CONSIDER_BIG = 400;
        DataAndImageTreeNode m = (DataAndImageTreeNode) input;
        m.traverse((DataAndImageTreeNode c) -> {
            counter.increment(c.childrenCount());
            if (counter.count > SIZE_TO_CONSIDER_BIG) {
                return false;
            }
            return true;
        });
        return counter.count > SIZE_TO_CONSIDER_BIG;
    }

}

final class Counter {
    public int count;

    public void increment(int childrenCount) {
        count += childrenCount;
    }

}