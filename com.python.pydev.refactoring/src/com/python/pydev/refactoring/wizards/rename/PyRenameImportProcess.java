package com.python.pydev.refactoring.wizards.rename;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

/**
 * Currently the same as the class rename.
 */
public class PyRenameImportProcess extends AbstractRenameRefactorProcess{

	public PyRenameImportProcess(Definition definition) {
		super(definition);
	}
	
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
		addOccurrences(request, getOccurrencesWithScopeAnalyzer(request));
    }

}
