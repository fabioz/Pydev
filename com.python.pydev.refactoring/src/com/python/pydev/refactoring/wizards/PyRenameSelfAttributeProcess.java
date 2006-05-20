/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameSelfAttributeProcess extends AbstractRenameRefactorProcess{

    private AssignDefinition assignDefinition;

    public PyRenameSelfAttributeProcess(Definition definition) {
        super(definition);
        this.assignDefinition = (AssignDefinition) definition;
    }

    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        ClassDef classDef = this.assignDefinition.scope.getClassDef();
        if(classDef == null){
            status.addFatalError("We're trying to rename an instance variable, but we cannot find a class definition.");
        }
        addOccurrences(request, ScopeAnalysis.getAttributeOcurrences(this.assignDefinition.target, classDef));
    }
}
