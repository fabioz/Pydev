package org.python.pydev.refactoring.codegenerator.constructorfield.request;

import java.util.List;

import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ConstructorFieldRequest implements IRefactoringRequest {

	private ClassDefAdapter classAdapter;

	private List<INodeAdapter> attributeAdapters;

	private int offsetStrategy;

	public ConstructorFieldRequest(ClassDefAdapter classAdapter, List<INodeAdapter> attributeAdapters, int offsetStrategy) {
		this.classAdapter = classAdapter;
		this.attributeAdapters = attributeAdapters;
		this.offsetStrategy = offsetStrategy;
	}

	public AbstractNodeAdapter getOffsetNode() {
		return classAdapter;
	}

	public List<INodeAdapter> getAttributeAdapters() {
		return attributeAdapters;
	}

	public ClassDefAdapter getClassAdapter() {
		return classAdapter;
	}

	public int getOffsetStrategy() {
		return offsetStrategy;
	}

}
