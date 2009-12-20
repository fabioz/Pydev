package org.python.pydev.core.structure;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T> {
 
    private T data;
    private final List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();
    private TreeNode<T> parent;
 
    public TreeNode(TreeNode<T> parent, T data) {
        this.parent = parent;
        if(parent != null){
            parent.addChild(this);
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

    public TreeNode<T> getParent() {
        return parent;
    }
}
