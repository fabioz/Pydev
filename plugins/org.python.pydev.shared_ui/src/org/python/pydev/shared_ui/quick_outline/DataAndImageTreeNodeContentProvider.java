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

public class DataAndImageTreeNodeContentProvider implements ITreeContentProvider {

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public Object[] getChildren(Object parentElement) {
        if (parentElement == null) {
            return new Object[0];
        }
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) parentElement;
        return m.childrenAsArray();
    }

    public Object getParent(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.getParent();
    }

    public boolean hasChildren(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.hasChildren();
    }

}
