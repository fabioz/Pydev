/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

public class DataAndImageTreeNode<X> {

    public final Image image;
    public final X data;
    public final List<DataAndImageTreeNode<X>> children = new ArrayList<DataAndImageTreeNode<X>>();
    public final DataAndImageTreeNode<X> parent;

    public DataAndImageTreeNode(DataAndImageTreeNode<X> parent, X data, Image image) {
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
    public DataAndImageTreeNode<X> createCopy(DataAndImageTreeNode<X> parent) {
        DataAndImageTreeNode<X> newRoot = new DataAndImageTreeNode<X>(parent, this.data, this.image);
        for (DataAndImageTreeNode<X> child : children) {
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
        if (!(obj instanceof DataAndImageTreeNode))
            return false;
        DataAndImageTreeNode other = (DataAndImageTreeNode) obj;
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
