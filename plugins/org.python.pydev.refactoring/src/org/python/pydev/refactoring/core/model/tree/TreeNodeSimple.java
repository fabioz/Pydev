/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.tree;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;

public class TreeNodeSimple<T extends INodeAdapter> implements ITreeNode {

    protected T adapter;

    private ITreeNode parent;

    public TreeNodeSimple(ITreeNode parent, T adapter) {
        this.adapter = adapter;
        this.parent = parent;
    }

    public ITreeNode getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return this.adapter.getName();
    }

    public String getLabel() {
        return this.toString();
    }

    public T getAdapter() {
        return adapter;
    }

    public Object[] getChildren() {
        return null;
    }

    public boolean hasChildren() {
        return getChildren() != null;
    }

    public String getImageName() {
        if (getAdapter() instanceof PropertyTextAdapter) {
            return ITreeNode.NODE_METHOD;
        } else {
            return ITreeNode.NODE_ATTRIBUTE;
        }
    }

}
