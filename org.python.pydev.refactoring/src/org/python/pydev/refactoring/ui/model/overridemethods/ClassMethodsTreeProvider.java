package org.python.pydev.refactoring.ui.model.overridemethods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ui.model.tree.ITreeNode;

public class ClassMethodsTreeProvider implements ITreeContentProvider {

	private List<ClassDefAdapter> classes;

	public ClassMethodsTreeProvider(List<ClassDefAdapter> adapters) {
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
		for (ClassDefAdapter elem : classes) {
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
