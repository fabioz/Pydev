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

public class TreeNode<T> {

    public T data;
    protected final LowMemoryArrayList<TreeNode<T>> children = new LowMemoryArrayList<TreeNode<T>>();
    private Object parent;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TreeNode(Object parent, T data) {
        this.parent = parent;
        if (parent instanceof TreeNode) {
            ((TreeNode) parent).addChild(this);
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
        return this.children.size() > 0;
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        fillBuf(this, buf, 0);
        return buf.toString();
    }

    private void fillBuf(TreeNode<T> treeNode, FastStringBuffer buf, int level) {
        buf.appendN("    ", level).append("TreeNode:").appendObject(treeNode.data).append('\n');
        for (TreeNode<T> child : treeNode.children) {
            fillBuf(child, buf, level + 1);
        }
    }

    public List<TreeNode<T>> flatten() {
        ArrayList<TreeNode<T>> array = new ArrayList<TreeNode<T>>(this.getChildren().size() + 10);
        collectChildren(array);
        return array;
    }

    private void collectChildren(ArrayList<TreeNode<T>> array) {
        List<TreeNode<T>> c = this.getChildren();
        int size = c.size();
        array.ensureCapacity(array.size() + size);
        for (int i = 0; i < size; i++) {
            TreeNode<T> next = c.get(i);
            array.add(next);
            next.collectChildren(array);
        }
    }

    public void clear() {
        this.children.clear();
    }

}
