/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.request;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ExtractLocalRequest implements IRefactoringRequest {

	private RefactoringInfo info;

	private String variableName;

	private exprType expression;

	public ExtractLocalRequest(RefactoringInfo info, exprType expression, String variableName) {
		this.info = info;
		this.expression = expression;
		this.variableName = variableName;
	}

	public int getOffsetStrategy() {
		return 0;
	}

	public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
		return info.getScopeAdapter();
	}
	
    public String getNewLineDelim() {
    	return info.getNewLineDelim();
    }
	
	public RefactoringInfo getRefactoringInfo() {
		return info;
	}

	public String getVariableName() {
		return variableName;
	}
	
	public exprType getExpression() {
		return expression;
	}
}
