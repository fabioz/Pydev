package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.ViewerSorter;

public class FileSorter extends ViewerSorter {
    public int category(Object element) {
        return ((File) element).isDirectory() ? 0 : 1;
    }
}