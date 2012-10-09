/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.sorter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.LabelAndImage;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.navigator.elements.PythonSourceFolder;

public class PythonModelSorter extends ViewerSorter {

    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof PythonNode || e2 instanceof PythonNode) {
            return 0; //we don't want to sort it... just show it in the order it is found in the file
        }

        //Tree nodes come after everything.
        if (e1 instanceof TreeNode<?>) {
            if (!(e2 instanceof TreeNode<?>)) {
                return 1;
            }
            //Both are ISortedElement, so, keep going.
        }
        if (e2 instanceof TreeNode<?>) {
            return -1;
        }

        //now, on to the priorities (if both have different classes)
        if (e1 instanceof ISortedElement && e2 instanceof ISortedElement) {
            ISortedElement iSortedElement1 = (ISortedElement) e1;
            int r1 = iSortedElement1.getRank();
            ISortedElement iSortedElement2 = (ISortedElement) e2;
            int r2 = iSortedElement2.getRank();
            if (r1 == r2) {
                if (e1 instanceof IWrappedResource && e2 instanceof IWrappedResource) {
                    return super.compare(viewer, ((IWrappedResource) e1).getActualObject(),
                            ((IWrappedResource) e2).getActualObject());

                } else if (e1 instanceof TreeNode && e2 instanceof TreeNode) {
                    TreeNode<?> p1 = (TreeNode<?>) e1;
                    TreeNode<?> p2 = (TreeNode<?>) e2;
                    Object data2 = p2.getData();
                    Object data1 = p1.getData();
                    if (data1 instanceof LabelAndImage && data2 instanceof LabelAndImage) {
                        return ((LabelAndImage) data1).label.compareTo(((LabelAndImage) data2).label);
                    }
                    return 0;

                } else {

                    return 0;
                }
            } else if (r1 < r2) {
                return -1;
            } else {
                return 1;
            }
        }
        //Config error elements always have priority over non-sorted resources
        if (e1 instanceof ProjectConfigError) {
            return -1;
        }
        if (e2 instanceof ProjectConfigError) {
            return 1;
        }

        //If both were IWrappedResource they'd be handled at ISortedElement
        if (e1 instanceof IWrappedResource) {
            //Source folders should come before other resources
            if (e1 instanceof PythonSourceFolder) {
                return -1;
            }
            e1 = ((IWrappedResource) e1).getActualObject();
        }
        if (e2 instanceof IWrappedResource) {
            //Source folders should come before other resources
            if (e2 instanceof PythonSourceFolder) {
                return 1;
            }
            e2 = ((IWrappedResource) e2).getActualObject();
        }

        if (e1 instanceof IContainer && e2 instanceof IContainer) {
            return super.compare(viewer, e1, e2);
        }
        if (e1 instanceof IContainer) {
            return -1;
        }
        if (e2 instanceof IContainer) {
            return 1;
        }

        return super.compare(viewer, e1, e2);
    }
}
