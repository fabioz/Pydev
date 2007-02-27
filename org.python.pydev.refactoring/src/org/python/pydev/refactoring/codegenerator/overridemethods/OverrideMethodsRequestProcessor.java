package org.python.pydev.refactoring.codegenerator.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.model.overridemethods.ClassTreeNode;
import org.python.pydev.refactoring.ui.model.overridemethods.FunctionTreeNode;

public class OverrideMethodsRequestProcessor implements IRequestProcessor<OverrideMethodsRequest> {

	private Object[] checked;

	private int insertionPoint;

	private boolean generateMethodComments;

	private ClassDefAdapter origin;

	public OverrideMethodsRequestProcessor(ClassDefAdapter origin) {
		checked = new Object[0];
		insertionPoint = IOffsetStrategy.AFTERINIT;
		this.origin = origin;
	}

	public void setCheckedElements(Object[] checked) {
		this.checked = checked;
	}

	public void setInsertionPoint(int strategy) {
		this.insertionPoint = strategy;
	}

	public void setGenerateMethodComments(boolean value) {
		this.generateMethodComments = value;
	}

	public List<OverrideMethodsRequest> getRefactoringRequests() {
		List<OverrideMethodsRequest> requests = new ArrayList<OverrideMethodsRequest>();

		for (ClassTreeNode clazz : getClasses()) {
			for (FunctionDefAdapter method : getMethods(clazz)) {
				requests.add(new OverrideMethodsRequest(origin, insertionPoint, method, generateMethodComments, clazz.getAdapter()
						.getName()));
			}
		}

		return requests;
	}

	private List<FunctionDefAdapter> getMethods(ClassTreeNode parent) {
		List<FunctionDefAdapter> methods = new ArrayList<FunctionDefAdapter>();

		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof FunctionTreeNode) {
				FunctionTreeNode method = (FunctionTreeNode) checked[i];
				if (method.getParent() == parent) {
					methods.add(method.getAdapter());
				}
			}
		}

		return methods;
	}

	private List<ClassTreeNode> getClasses() {
		List<ClassTreeNode> classes = new ArrayList<ClassTreeNode>();

		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof ClassTreeNode) {
				classes.add((ClassTreeNode) checked[i]);
			}
		}

		return classes;
	}

}
