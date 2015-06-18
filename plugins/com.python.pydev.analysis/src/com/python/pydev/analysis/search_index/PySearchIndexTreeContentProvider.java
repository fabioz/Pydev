/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.TreeViewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.search.AbstractSearchIndexResultPage;
import org.python.pydev.shared_ui.search.AbstractSearchIndexTreeContentProvider;

/**
 * This is a content provider that creates a separate structure based on TreeNodes
 * so that we can have better control on how to show things.
 */
public class PySearchIndexTreeContentProvider extends AbstractSearchIndexTreeContentProvider {

    public PySearchIndexTreeContentProvider(AbstractSearchIndexResultPage searchIndexResultPage, TreeViewer viewer) {
        super(viewer);
    }

    @Override
    protected TreeNode<?> obtainTeeNodeElement(final Object object) {
        if (object instanceof TreeNode) {
            return (TreeNode<?>) object;
        }

        TreeNode<?> treeNode = elementToTreeNode.get(object);
        if (treeNode != null) {
            return treeNode;
        }

        TreeNode<?> ret = null;
        if (object instanceof PyModuleLineElement) {
            PyModuleLineElement moduleLineElement = (PyModuleLineElement) object;
            TreeNode<?> parentNode;

            if ((this.groupWith & GROUP_WITH_MODULES) != 0) {
                parentNode = obtainTeeNodeElement(new PyCustomModule(moduleLineElement));
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                IResource parent = moduleLineElement.getParent();
                parentNode = obtainTeeNodeElement(parent);
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                parentNode = obtainTeeNodeElement(moduleLineElement.getProject());
                ret = new TreeNode<>(parentNode, object);

            } else {
                // No grouping at all (flat)
                ret = new TreeNode<>(root, object);

            }

        } else if (object instanceof PyCustomModule) {
            if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                PyCustomModule package1 = (PyCustomModule) object;
                TreeNode<?> parentNode = obtainTeeNodeElement(package1.resource.getParent());
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                PyCustomModule package1 = (PyCustomModule) object;
                TreeNode<?> parentNode = obtainTeeNodeElement(package1.project);
                ret = new TreeNode<>(parentNode, object);

            } else {
                // Already at root
                ret = new TreeNode<>(root, object);

            }

        } else if (object instanceof IProject) {
            // Projects are always beneath root
            ret = new TreeNode<>(root, object);

        } else if (object instanceof IResource) {
            if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                // If we got a resource use its parent
                IResource resource = (IResource) object;
                IContainer parent = resource.getParent();
                if (parent instanceof IProject) {
                    if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                        TreeNode<?> parentNode = obtainTeeNodeElement(parent);
                        ret = new TreeNode<>(parentNode, object);
                    } else {
                        // Already at root
                        ret = new TreeNode<>(root, object);
                    }
                } else {
                    TreeNode<?> parentNode = obtainTeeNodeElement(parent);
                    ret = new TreeNode<>(parentNode, object);
                }

            } else {
                // Already at root
                ret = new TreeNode<>(root, object);
            }
        }

        if (ret == null) {
            Log.log("Unhandled: " + object + " group by: " + this.groupWith);
            return null;
        }
        elementToTreeNode.put(object, ret);

        return ret;
    }

}
