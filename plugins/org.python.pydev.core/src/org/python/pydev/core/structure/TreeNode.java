/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.structure;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T> {

    private T data;
    private final List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();
    private Object parent;

    public TreeNode(Object parent, T data) {
        this.parent = parent;
        if (parent != null) {
            if (parent instanceof TreeNode) {
                ((TreeNode) parent).addChild(this);
            }
        }
        setData(data);
    }

    public List<TreeNode<T>> getChildren() {
        return this.children;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    private void addChild(TreeNode<T> treeNode) {
        this.children.add(treeNode);
    }

    public Object getParent() {
        return parent;
    }

    public boolean hasChildren() {
        return this.getChildren().size() > 0;
    }
}
