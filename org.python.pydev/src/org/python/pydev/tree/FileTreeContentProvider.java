package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class FileTreeContentProvider implements ITreeContentProvider {
    public Object[] getChildren(Object element) {
        Object[] kids = ((File) element).listFiles();
        return kids == null ? new Object[0] : kids;
    }

    public Object[] getElements(Object element) {
        return getChildren(element);
    }

    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    public Object getParent(Object element) {
        if (element == null) {
            return null;
        }

        if (element instanceof File) {
            ((File) element).getParent();

        }
        System.out.println("element not instance of File: " + element.getClass().getName() + " "
                + element.toString());
        return null;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
    }
}