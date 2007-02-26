package org.python.pydev.refactoring.core.request;

import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public interface IRefactoringRequest {

	public abstract IASTNodeAdapter getOffsetNode();

}