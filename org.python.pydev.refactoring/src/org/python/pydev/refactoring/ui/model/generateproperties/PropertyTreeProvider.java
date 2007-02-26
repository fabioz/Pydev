package org.python.pydev.refactoring.ui.model.generateproperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ui.model.tree.ITreeNode;

public class PropertyTreeProvider implements ITreeContentProvider {

	private List<ClassDefAdapter> adapters;

	public PropertyTreeProvider(List<ClassDefAdapter> adapters) {
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
		for (ClassDefAdapter elem : adapters) {
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
