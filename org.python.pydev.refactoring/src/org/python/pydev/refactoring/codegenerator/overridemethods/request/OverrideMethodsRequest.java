/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.overridemethods.request;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class OverrideMethodsRequest implements IRefactoringRequest {

	private IClassDefAdapter classAdapter;

	private FunctionDefAdapter method;

	private int offsetStrategy;

	private boolean generateMethodComments;

	private String baseClassName;

    private String endLineDelim;

	public OverrideMethodsRequest(IClassDefAdapter classAdapter, int offsetStrategy, FunctionDefAdapter method,
			boolean generateMethodComments, String baseClassName, String endLineDelim) {
		this.baseClassName = baseClassName;
		this.classAdapter = classAdapter;
		this.offsetStrategy = offsetStrategy;
		this.method = method;
		this.generateMethodComments = generateMethodComments;
        this.endLineDelim = endLineDelim;
	}

	public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
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
		return getOffsetNode().getModule().getBaseContextName(this.classAdapter, baseClassName);
	}

    public String getNewLineDelim() {
        return endLineDelim;
    }
}
