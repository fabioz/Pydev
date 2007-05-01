/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Trivial: A ContentProvider interface ParsedItem tree items
 * 
 */
public class ParsedContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object inputElement) {
        return ((ParsedItem) inputElement).getChildren();
    }

    public void dispose() {
    }

    public Object[] getChildren(Object parentElement) {
        return ((ParsedItem) parentElement).getChildren();
    }

    public Object getParent(Object element) {
        return ((ParsedItem) element).getParent();
    }

    public boolean hasChildren(Object element) {
        return (((ParsedItem) element).getChildren().length > 0);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}
