/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyRenameLocalProcess extends AbstractRenameRefactorProcess{


    public PyRenameLocalProcess(Definition definition) {
        super(definition);
    }


    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        addOccurrences(request, definition.scope.getOcurrences(request.duringProcessInfo.initialName, definition.module));
    }

    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        if(!definition.module.getName().equals(request.moduleName)){
        	//it was found in another module, but we want to keep things local
        	addOccurrences(request, Scope.getOcurrences(request.duringProcessInfo.initialName, request.getAST()));
        }else{
            checkInitialOnWorkspace(status, request);
        }
    }


}
