/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameSelfAttributeProcess extends AbstractRenameWorkspaceRefactorProcess{

    /**
     * Target is 'self.attr'
     */
    private String target;

	public PyRenameSelfAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }

    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        addOccurrences(request, ScopeAnalysis.getAttributeReferences(request.initialName, request.getAST()));
    }

    @Override
    protected List<ASTEntry> getEntryOccurrences(RefactoringStatus status, String initialName, SourceModule module) {
        return ScopeAnalysis.getAttributeReferences(initialName, module.getAst()); //will get the self.xxx occurrences
    }
    
    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

}
