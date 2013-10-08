/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.navigator.LabelAndImage;
import org.python.pydev.shared_core.structure.TreeNode;

public abstract class AbstractFilter extends ViewerFilter {

    protected String getName(Object element) {
        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IResource.class);
            if (adapted instanceof IResource) {
                IResource resource = (IResource) adapted;
                return resource.getName();
            }

        } else if (element instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) element;
            Object data = treeNode.getData();
            if (data instanceof LabelAndImage) {
                return ((LabelAndImage) data).label;
            }
        }
        return null;
    }

}
