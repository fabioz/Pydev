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

package org.python.pydev.refactoring.core.model.overridemethods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;

public class ClassMethodsTreeProvider implements ITreeContentProvider {

    private List<IClassDefAdapter> classes;

    public ClassMethodsTreeProvider(List<IClassDefAdapter> adapters) {
        this.classes = adapters;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return ((ITreeNode) parentElement).getChildren();
    }

    @Override
    public Object getParent(Object element) {
        return ((ITreeNode) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        ITreeNode node = (ITreeNode) element;
        return node.hasChildren();
    }

    @Override
    public Object[] getElements(Object inputElement) {
        Collection<ClassTreeNode> elements = new ArrayList<ClassTreeNode>();
        for (IClassDefAdapter elem : classes) {
            if (elem.hasFunctionsInitFiltered()) {
                elements.add(new ClassTreeNode(elem));
            }
        }
        return elements.toArray();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
