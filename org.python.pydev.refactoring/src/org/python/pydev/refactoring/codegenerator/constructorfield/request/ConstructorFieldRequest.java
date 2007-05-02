package org.python.pydev.refactoring.codegenerator.constructorfield.request;

import java.util.List;

import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ConstructorFieldRequest implements IRefactoringRequest {

	private IClassDefAdapter classAdapter;

	private List<INodeAdapter> attributeAdapters;

	private int offsetStrategy;

    private String newLineDelim;

	public ConstructorFieldRequest(IClassDefAdapter classAdapter, List<INodeAdapter> attributeAdapters, int offsetStrategy, String newLineDelim) {
		this.classAdapter = classAdapter;
		this.attributeAdapters = attributeAdapters;
		this.offsetStrategy = offsetStrategy;
        this.newLineDelim = newLineDelim;
	}

	public IASTNodeAdapter getOffsetNode() {
		return classAdapter;
	}

	public List<INodeAdapter> getAttributeAdapters() {
		return attributeAdapters;
	}

	public IClassDefAdapter getClassAdapter() {
		return classAdapter;
	}

	public int getOffsetStrategy() {
		return offsetStrategy;
	}

    public String getNewLineDelim() {
        return newLineDelim;
    }

}
