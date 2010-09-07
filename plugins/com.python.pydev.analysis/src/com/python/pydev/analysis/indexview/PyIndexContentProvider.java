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
        return ((ITreeElement)inputElement).getChildren();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return ((ITreeElement)parentElement).getChildren();
    }

    @Override
    public Object getParent(Object element) {
        return ((ITreeElement)element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        return ((ITreeElement)element).hasChildren();
    }


}
