/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;

public class PyRenameAttributeProcess extends AbstractRenameRefactorProcess{

    private AssignDefinition assignDefinition;

    public PyRenameAttributeProcess(Definition definition) {
        super(definition);
        this.assignDefinition = (AssignDefinition) definition;
    }

    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        ClassDef classDef = this.assignDefinition.scope.getClassDef();
        if(classDef == null){
            status.addFatalError("We're trying to rename an instance variable, but we cannot find a class definition.");
        }
        addOccurrences(request, Scope.getAttributeOcurrences(this.assignDefinition.target, classDef));
    }
}
