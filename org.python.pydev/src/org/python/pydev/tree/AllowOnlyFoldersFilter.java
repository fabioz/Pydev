package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class AllowOnlyFoldersFilter extends ViewerFilter {
    public boolean select(Viewer viewer, Object parent, Object element) {
        return ((File) element).isDirectory();
    }

}