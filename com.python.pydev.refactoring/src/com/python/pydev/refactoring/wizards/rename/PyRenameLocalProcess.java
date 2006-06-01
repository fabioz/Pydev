/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameLocalProcess extends AbstractRenameRefactorProcess{


    public PyRenameLocalProcess(Definition definition) {
        super(definition);
    }


    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, definition.module, definition.scope));
    }

    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        if(!definition.module.getName().equals(request.moduleName)){
        	//it was found in another module, but we want to keep things local
        	addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, request.getAST()));
        }else{
            checkInitialOnWorkspace(status, request);
        }
    }


}
