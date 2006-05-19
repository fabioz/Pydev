package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;


public class PyRenameAnyLocalProcess extends AbstractRenameRefactorProcess{

	/**
	 * No definition (will look for the name)
	 */
	public PyRenameAnyLocalProcess() {
		super(null);
	}
	
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
    	List<ASTEntry> oc = getOccurrencesWithScopeAnalyzer(request);
    	if(oc.size() == 0){
    		addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, request.getAST(), false));
    	}else{
    		addOccurrences(request, oc);
    	}
    }
}
