package com.python.pydev.refactoring.wizards;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;


public class PyRenameAnyLocalProcess extends AbstractRenameRefactorProcess{

	/**
	 * No definition (will look for the name)
	 */
	public PyRenameAnyLocalProcess() {
		super(null);
	}
	
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
    	addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, request.getAST(), false));
    }
}
