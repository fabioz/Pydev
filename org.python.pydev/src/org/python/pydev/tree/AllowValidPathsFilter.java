/*
 * Created on Oct 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * @author Fabio Zadrozny
 */
public class AllowValidPathsFilter extends ViewerFilter {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        return ((File) element).exists();
    }

}