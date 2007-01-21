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


    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.initialName, definition.module, definition.scope));
    }

    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        if(!definition.module.getName().equals(request.moduleName)){
        	//it was found in another module, but we want to keep things local
        	addOccurrences(request, ScopeAnalysis.getLocalOcurrences(request.initialName, request.getAST()));
        }else{
            findReferencesToRenameOnWorkspace(request, status);
        }
    }


}
