/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.hierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

public class TreeNode<X> {

    public final Image image;
    public final X data;
    public final List<TreeNode<X>> children = new ArrayList<TreeNode<X>>();
    public final TreeNode<X> parent;

    public TreeNode(TreeNode<X> parent, X data, Image image) {
        this.parent = parent;
        this.data = data;
        if (parent != null) {
            parent.children.add(this);
        }
        this.image = image;
    }

    /**
     * @return a copy of the tree node structure (nodes copied, same data and image)
     */
    public TreeNode<X> createCopy(TreeNode<X> parent) {
        TreeNode<X> newRoot = new TreeNode<X>(parent, this.data, this.image);
        for (TreeNode<X> child : children) {
            child.createCopy(newRoot);
        }

        return newRoot;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((image == null) ? 0 : image.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TreeNode))
            return false;
        TreeNode other = (TreeNode) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (image == null) {
            if (other.image != null)
                return false;
        } else if (!image.equals(other.image))
            return false;
        return true;
    }

}
