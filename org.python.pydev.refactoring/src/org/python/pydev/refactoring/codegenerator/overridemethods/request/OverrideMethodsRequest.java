package org.python.pydev.refactoring.codegenerator.overridemethods.request;

import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class OverrideMethodsRequest implements IRefactoringRequest {

	private ClassDefAdapter classAdapter;

	private FunctionDefAdapter method;

	private int offsetStrategy;

	private boolean generateMethodComments;

	private String baseClassName;

	public OverrideMethodsRequest(ClassDefAdapter classAdapter,
			int offsetStrategy, FunctionDefAdapter method,
			boolean generateMethodComments, String baseClassName) {
		this.baseClassName = baseClassName;
		this.classAdapter = classAdapter;
		this.offsetStrategy = offsetStrategy;
		this.method = method;
		this.generateMethodComments = generateMethodComments;
	}

	public AbstractNodeAdapter getOffsetNode() {
		return classAdapter;
	}

	public FunctionDefAdapter getFunctionAdapter() {
		return method;
	}

	public int getOffsetStrategy() {
		return offsetStrategy;
	}

	public boolean getGenerateMethodComments() {
		return generateMethodComments;
	}

	public String getBaseClassName() {
		return getOffsetNode().getModule().getBaseContextName(
				this.classAdapter, baseClassName);
	}
}
