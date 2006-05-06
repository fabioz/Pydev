/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameClassProcess extends AbstractRenameRefactorProcess{

    public PyRenameClassProcess(Definition definition) {
        super(definition);
    }

    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        SimpleNode root = request.getAST();
        List<ASTEntry> oc = Scope.getOcurrences(request.duringProcessInfo.initialName, root);
        addOccurrences(request, oc);
    }
}
