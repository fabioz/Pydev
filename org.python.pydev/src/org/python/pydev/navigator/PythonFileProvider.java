/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class PythonFileProvider implements ITreeContentProvider{
    private static final Object[] NO_CHILDREN = new Object[0];
    
    public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof IFile){
            return new Object[]{new PythonTreeData((IFile) parentElement)};
        }
        return NO_CHILDREN;
    }

    public Object getParent(Object element) {
        if(element instanceof PythonTreeData){
            PythonTreeData data = (PythonTreeData) element;
            return data.parentElement;
        }
        return null;
    }

    public boolean hasChildren(Object element) {
        return true;
    }

    /**
     * Get the roots
     */
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
