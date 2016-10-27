package org.python.pydev.ui.dialogs;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ListContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(Object element) {
        if (element instanceof List) {
            List list = (List) element;
            return list.toArray();
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return element instanceof List && ((List) element).size() > 0;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public void dispose() {
        //do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}
