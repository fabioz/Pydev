package org.python.pydev.shared_core.structure;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TreeNodeContentProvider implements ITreeContentProvider {

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
        return m.children.toArray();
    }

    public Object getParent(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.parent;
    }

    public boolean hasChildren(Object element) {
        @SuppressWarnings("rawtypes")
        DataAndImageTreeNode m = (DataAndImageTreeNode) element;
        return m.children.size() > 0;
    }

}
