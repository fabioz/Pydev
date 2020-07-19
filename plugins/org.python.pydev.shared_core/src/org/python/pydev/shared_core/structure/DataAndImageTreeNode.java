/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.structure;

import org.python.pydev.shared_core.image.IImageHandle;

@SuppressWarnings("unchecked")
public class DataAndImageTreeNode<X> extends TreeNode<X> {

    public final IImageHandle image;

    public DataAndImageTreeNode(DataAndImageTreeNode<X> parent, X data, IImageHandle image) {
        super(parent, data);
        this.image = image;
    }

    /**
     * @return a copy of the tree node structure (nodes copied, same data and image)
     */
    public DataAndImageTreeNode<X> createCopy(DataAndImageTreeNode<X> parent) {
        DataAndImageTreeNode<X> newRoot = new DataAndImageTreeNode<X>(parent, this.getData(), this.image);
        for (TreeNode<X> child : children) {
            ((DataAndImageTreeNode<X>) child).createCopy(newRoot);
        }

        return newRoot;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        X localData = this.getData();
        result = prime * result + ((localData == null) ? 0 : localData.hashCode());
        result = prime * result + ((image == null) ? 0 : image.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataAndImageTreeNode)) {
            return false;
        }
        DataAndImageTreeNode other = (DataAndImageTreeNode) obj;
        X localData = getData();
        if (localData == null) {
            if (other.getData() != null) {
                return false;
            }
        } else if (!localData.equals(other.getData())) {
            return false;
        }
        if (image == null) {
            if (other.image != null) {
                return false;
            }
        } else if (!image.equals(other.image)) {
            return false;
        }
        return true;
    }

    public Object[] childrenAsArray() {
        return this.children.toArray();
    }

    public int childrenCount() {
        return this.children.size();
    }

    @SuppressWarnings("rawtypes")
    public DataAndImageTreeNode childAt(int i) {
        return (DataAndImageTreeNode) this.children.get(i);
    }

    public static interface ITraverser {

        boolean traverse(@SuppressWarnings("rawtypes") DataAndImageTreeNode dataAndImageTreeNode);

    }

    /**
     * Traverse all the items in the nodes structure (until the traverser returns "false".).
     */
    @SuppressWarnings("rawtypes")
    public boolean traverse(ITraverser traverser) {
        boolean continueTraverse = traverser.traverse(this);
        if (continueTraverse) {
            for (Object o : this.children) {
                continueTraverse = ((DataAndImageTreeNode) o).traverse(traverser);
                if (!continueTraverse) {
                    return false;
                }
            }
        }
        return continueTraverse;
    }

}
