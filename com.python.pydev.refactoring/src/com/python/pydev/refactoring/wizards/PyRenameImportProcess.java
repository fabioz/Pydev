package com.python.pydev.refactoring.wizards;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;

/**
 * Currently the same as the class rename.
 */
public class PyRenameImportProcess extends PyRenameClassProcess{

	public PyRenameImportProcess(Definition definition) {
		super(definition);
	}
	
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        IModule module = request.getModule();
		ScopeAnalyzerVisitor visitor = new ScopeAnalyzerVisitor(request.nature, request.moduleName, 
        		module, request.doc, new NullProgressMonitor(), request.ps);
        try {
			request.getAST().accept(visitor);
			addOccurrences(request, visitor.getEntryOccurrences());
		} catch (Exception e) {
			Log.log(e);
		}
        
    }

}
