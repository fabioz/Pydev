/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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

    @Override
    public ITreeNode getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return this.adapter.getName();
    }

    @Override
    public String getLabel() {
        return this.toString();
    }

    @Override
    public T getAdapter() {
        return adapter;
    }

    @Override
    public Object[] getChildren() {
        return null;
    }

    @Override
    public boolean hasChildren() {
        return getChildren() != null;
    }

    @Override
    public String getImageName() {
        if (getAdapter() instanceof PropertyTextAdapter) {
            return ITreeNode.NODE_METHOD;
        } else {
            return ITreeNode.NODE_ATTRIBUTE;
        }
    }

}
