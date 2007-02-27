package org.python.pydev.refactoring.codegenerator.constructorfield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.model.constructorfield.TreeNodeClassField;
import org.python.pydev.refactoring.ui.model.constructorfield.TreeNodeField;
import org.python.pydev.refactoring.ui.model.tree.ITreeNode;

public class ConstructorFieldRequestProcessor implements IRequestProcessor<ConstructorFieldRequest> {

	private Object[] checked;

	private int offsetStrategy;

	public ConstructorFieldRequestProcessor() {
		checked = new Object[0];
		offsetStrategy = IOffsetStrategy.AFTERINIT;
	}

	public void setCheckedElements(Object[] checked) {
		this.checked = checked;
	}

	public List<ConstructorFieldRequest> getRefactoringRequests() {
		return generateRequests();
	}

	private List<ConstructorFieldRequest> generateRequests() {
		List<ConstructorFieldRequest> requests = new ArrayList<ConstructorFieldRequest>();
		List<ITreeNode> nodes = new ArrayList<ITreeNode>();
		for (Object o : checked) {
			nodes.add((ITreeNode) o);
		}

		Iterator<ITreeNode> iter = nodes.iterator();
		while (iter.hasNext()) {
			ITreeNode node = iter.next();
			if (node instanceof TreeNodeClassField) {
				addRequest(requests, iter, node);
			}
		}

		return requests;
	}

	private void addRequest(List<ConstructorFieldRequest> requests, Iterator<ITreeNode> iter, ITreeNode node) {
		List<INodeAdapter> fields = new ArrayList<INodeAdapter>();
		ITreeNode field = iter.next();
		while (field instanceof TreeNodeField) {
			fields.add((SimpleAdapter) field.getAdapter());
			if (iter.hasNext())
				field = iter.next();
			else
				break;
		}
		if (fields.size() > 0) {
			ClassDefAdapter clazz = (ClassDefAdapter) node.getAdapter();
			ConstructorFieldRequest request = new ConstructorFieldRequest(clazz, fields, offsetStrategy);
			requests.add(request);
		}
	}

	public void setMethodDestination(int strat) {
		this.offsetStrategy = strat;
	}

}
