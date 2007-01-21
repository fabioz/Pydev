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
	
    protected void checkInitialOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        String[] tokenAndQual = request.ps.getActivationTokenAndQual(true);
        String completeNameToFind = tokenAndQual[0]+tokenAndQual[1];
        boolean attributeSearch = completeNameToFind.indexOf('.') != -1;
            
        if (!attributeSearch){
            List<ASTEntry> oc = getOccurrencesWithScopeAnalyzer(request);
            if(oc.size() > 0){
                addOccurrences(request, oc); 
            }else{
                addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.initialName, request.getAST(), false));
            }
            return;
            
        }else{
            //attribute search
            addOccurrences(request, ScopeAnalysis.getAttributeReferences(request.initialName, request.getAST()));
        }
    }
}
