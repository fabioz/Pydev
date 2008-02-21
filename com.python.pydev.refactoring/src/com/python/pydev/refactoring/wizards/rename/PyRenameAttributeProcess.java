/*
 * Created on May 20, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.AttributeReferencesVisitor;
import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameAttributeProcess extends AbstractRenameWorkspaceRefactorProcess{

    /**
     * Target is the full name. E.g.: foo.bar (and the initialName would be just 'bar')
     */
	private String target;

    public PyRenameAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }


    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode ast = request.getAST();
		
        List<ASTEntry> attributeOccurrences = new ArrayList<ASTEntry>(); 
        attributeOccurrences.addAll(ScopeAnalysis.getAttributeOccurrences(this.target, ast));
		attributeOccurrences.addAll(ScopeAnalysis.getAttributeReferences(this.target, ast));
		if(attributeOccurrences.size() > 0){
			//only add comments and strings if there's at least some other occurrence
	        attributeOccurrences.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, ast));
	        attributeOccurrences.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, ast));
		}
		addOccurrences(request, attributeOccurrences);
    }


    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, String initialName, SourceModule module) {
        return ScopeAnalysis.getAttributeReferences(initialName, module.getAst()); //will get the self.xxx occurrences
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

}
