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

    public Object[] getChildren(Object parentElement) {
        return ((ITreeNode) parentElement).getChildren();
    }

    public Object getParent(Object element) {
        return ((ITreeNode) element).getParent();
    }

    public boolean hasChildren(Object element) {
        ITreeNode node = (ITreeNode) element;
        return node.hasChildren();
    }

    public Object[] getElements(Object inputElement) {
        Collection<ClassTreeNode> elements = new ArrayList<ClassTreeNode>();
        for (IClassDefAdapter elem : classes) {
            if (elem.hasFunctionsInitFiltered()) {
                elements.add(new ClassTreeNode(elem));
            }
        }
        return elements.toArray();
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
