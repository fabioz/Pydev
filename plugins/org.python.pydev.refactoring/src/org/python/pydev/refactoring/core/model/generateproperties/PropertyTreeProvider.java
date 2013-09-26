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
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.generateproperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;

public class PropertyTreeProvider implements ITreeContentProvider {

    private List<IClassDefAdapter> adapters;

    public PropertyTreeProvider(List<IClassDefAdapter> adapters) {
        this.adapters = adapters;
    }

    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ITreeNode) {
            return ((ITreeNode) parentElement).getChildren();
        }
        return null;
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof ITreeNode) {
            ITreeNode node = (ITreeNode) element;
            return node.hasChildren();
        }
        return false;
    }

    public Object[] getElements(Object inputElement) {
        Collection<TreeClassNode> elements = new ArrayList<TreeClassNode>();
        for (IClassDefAdapter elem : adapters) {
            if (elem.hasAttributes()) {
                elements.add(new TreeClassNode(elem));
            }
        }
        return elements.toArray();
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
