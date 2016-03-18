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

package org.python.pydev.refactoring.core.model.constructorfield;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;

public class ClassFieldTreeProvider implements ITreeContentProvider {

    private IClassDefAdapter rootClass;

    public ClassFieldTreeProvider(IClassDefAdapter rootClass) {
        this.rootClass = rootClass;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ITreeNode) {
            return ((ITreeNode) parentElement).getChildren();
        }
        return null;
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof ITreeNode) {
            ITreeNode node = (ITreeNode) element;
            return node.hasChildren();
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return new Object[] { new TreeNodeClassField(rootClass) };
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
