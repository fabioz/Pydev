/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.sorter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.python.pydev.navigator.PythonLabelProvider;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.shared_core.structure.TreeNode;

/**
 * @author Fabio Zadrozny
 */
public class PythonModelSorter extends ViewerSorter {

    private PythonLabelProvider labelProvider;

    public PythonModelSorter() {
        labelProvider = new PythonLabelProvider();
    }

    @Override
    public int category(Object element) {
        if (element instanceof TreeNode) {
            return ISortedElement.RANK_TREE_NODE;
        }

        if (element instanceof ISortedElement) {
            ISortedElement iSortedElement = (ISortedElement) element;
            return iSortedElement.getRank();
        }

        if (element instanceof IContainer) {
            return ISortedElement.RANK_REGULAR_FOLDER;
        }
        if (element instanceof IFile) {
            return ISortedElement.RANK_REGULAR_FILE;
        }
        if (element instanceof IResource) {
            return ISortedElement.RANK_REGULAR_RESOURCE;
        }
        return ISortedElement.UNKNOWN_ELEMENT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof PythonNode && e2 instanceof PythonNode) {
            return 0; //we don't want to sort it... just show it in the order it is found in the file
        }

        //Could be super.compare, but we don't have a way to override getLabel, so, copying the whole code. 
        int cat1 = category(e1);
        int cat2 = category(e2);

        if (cat1 != cat2) {
            return cat1 - cat2;
        }

        String name1 = getLabel(viewer, e1);
        String name2 = getLabel(viewer, e2);

        // use the comparator to compare the strings
        int compare = getComparator().compare(name1, name2);
        return compare;
    }

    private String getLabel(Viewer viewer, Object e1) {
        String text = labelProvider.getText(e1);
        return text;
    }
}
