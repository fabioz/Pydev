package com.python.pydev.refactoring.wizards.rename;

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
            String[] tokenAndQual = request.ps.getActivationTokenAndQual(true);
            String completeNameToFind = tokenAndQual[0]+tokenAndQual[1];

            if(completeNameToFind.indexOf('.') == -1){
                addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, request.getAST(), false));
            }else{
                addOccurrences(request, ScopeAnalysis.getAttributeReferences(request.duringProcessInfo.initialName, request.getAST()));
            }
    	}else{
    		addOccurrences(request, oc);
    	}
    }
}
