/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyRenameLocalProcess extends AbstractRefactorProcess{


    public PyRenameLocalProcess(Definition definition) {
        super(definition);
    }

    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        super.checkInitialConditions(pm, status, request);
    	if(request.findReferencesOnlyOnLocalScope){
    		if(!definition.module.getName().equals(request.moduleName)){
    			//it was found in another module, but we want to keep things local
    			addOccurrences(request, Scope.getOcurrences(request.duringProcessInfo.initialName, request.getAST()));
            }
    	}
    	
    	if(this.occurrences.size() == 0){
    		//not looked previously
	        Scope scope = definition.scope;
            addOccurrences(request, scope.getOcurrences(request.duringProcessInfo.initialName, definition.module));
    	}
    	
        if(this.occurrences.size() == 0){
            status.addFatalError("Could not find any occurrences of:"+request.duringProcessInfo.initialName);
        }
    }


}
