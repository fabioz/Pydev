/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.structure;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Note: equals and hashCode are identity based (i.e.: Object implementation).
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TreeNode<T> {

    public T data;

    /**
     * Note: children of a class don't necessarily have the same type as the parent.
     */
    protected final LowMemoryArrayList<TreeNode> children = new LowMemoryArrayList<TreeNode>();
    private Object parent;

    public TreeNode(Object parent, T data) {
        this.parent = parent;
        if (parent instanceof TreeNode) {
            ((TreeNode) parent).addChild(this);
        }
        setData(data);
    }

    public List<TreeNode> getChildren() {
        return this.children;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    private void addChild(TreeNode treeNode) {
        this.children.add(treeNode);
    }

    public Object getParent() {
        return parent;
    }

    public boolean hasChildren() {
        return this.children.size() > 0;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    // To use while debugging
    public String toStringRepr() {
        FastStringBuffer buf = new FastStringBuffer();
        fillBuf(this, buf, 0);
        return buf.toString();
    }

    protected void fillBuf(TreeNode<T> treeNode, FastStringBuffer buf, int level) {
        buf.appendN("    ", level).append("TreeNode:").appendObject(treeNode.data).append('\n');
        for (TreeNode<T> child : treeNode.children) {
            fillBuf(child, buf, level + 1);
        }
    }

    public <Y> List<TreeNode<Y>> flatten() {
        ArrayList<TreeNode<Y>> array = new ArrayList<TreeNode<Y>>(this.getChildren().size() + 10);
        collectChildren(array);
        return array;
    }

    private <Y> void collectChildren(ArrayList<TreeNode<Y>> array) {
        List<TreeNode> c = this.getChildren();
        int size = c.size();
        array.ensureCapacity(array.size() + size);
        for (int i = 0; i < size; i++) {
            TreeNode<Y> next = c.get(i);
            array.add(next);
            next.collectChildren(array);
        }
    }

    public void clear() {
        this.children.clear();
    }

}
