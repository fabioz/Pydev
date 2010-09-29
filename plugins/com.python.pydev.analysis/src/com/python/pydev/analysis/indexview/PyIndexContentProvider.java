package com.python.pydev.analysis.indexview;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PyIndexContentProvider implements ITreeContentProvider {

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    
    public Object[] getElements(Object inputElement) {
        return ((ITreeElement)inputElement).getChildren();
    }

    
    public Object[] getChildren(Object parentElement) {
        return ((ITreeElement)parentElement).getChildren();
    }

    
    public Object getParent(Object element) {
        return ((ITreeElement)element).getParent();
    }

    
    public boolean hasChildren(Object element) {
        return ((ITreeElement)element).hasChildren();
    }


}
