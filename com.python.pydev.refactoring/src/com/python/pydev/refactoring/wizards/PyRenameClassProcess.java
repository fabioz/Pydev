/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameClassProcess extends AbstractRefactorProcess{

    public PyRenameClassProcess(Definition definition) {
        super(definition);
    }

    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        this.request = request;
        if(request.findReferencesOnlyOnLocalScope == true){
            SimpleNode root = request.getAST();
            List<ASTEntry> oc = Scope.getOcurrences(request.duringProcessInfo.initialName, root);
            if(oc.size() == 0){
                status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
                return;
            }
            addOccurrences(request, oc);
        }else{
            throw new RuntimeException("Currently can only get things in the local scope.");
        }
    }
}
