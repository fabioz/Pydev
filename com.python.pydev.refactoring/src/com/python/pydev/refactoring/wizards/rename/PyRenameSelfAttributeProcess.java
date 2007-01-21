/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameSelfAttributeProcess extends AbstractRenameRefactorProcess{


    private String target;

	public PyRenameSelfAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }

    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        ClassDef classDef = ((LocalScope)this.definition.scope).getClassDef();
        if(classDef == null){
            status.addFatalError("We're trying to rename an instance variable, but we cannot find a class definition.");
        }
        addOccurrences(request, ScopeAnalysis.getAttributeOcurrences(target, classDef));
    }
}
