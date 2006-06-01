/*
 * Created on May 20, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameAttributeProcess extends AbstractRenameRefactorProcess{

	private String target;

    public PyRenameAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }


    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        SimpleNode ast = request.getAST();
		
        List<ASTEntry> attributeOcurrences = ScopeAnalysis.getAttributeOcurrences(this.target, ast);
		addOccurrences(request, attributeOcurrences);
		
		attributeOcurrences = ScopeAnalysis.getAttributeReferences(this.target, ast);
		addOccurrences(request, attributeOcurrences);
    }

}
