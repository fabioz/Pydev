package com.python.pydev.refactoring.wizards.rename;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * The rename import process is used when we find that we have to rename a module
 * (because we're renaming an import to the module).
 * 
 * @see RefactorProcessFactory#getProcess(Definition)
 * 
 * Currently we do not support this type of refactoring for global refactorings (it always
 * acts locally).
 */
public class PyRenameImportProcess extends AbstractRenameRefactorProcess{

    public static final int TYPE_RENAME_MODULE = 1;
    public static final int TYPE_RENAME_UNRESOLVED_IMPORT = 2;
    
    protected int type=-1;
    
    /**
     * @param definition this is the definition we're interested in.
     */
	public PyRenameImportProcess(Definition definition) {
		super(definition);
        if(definition.ast == null){
            this.type = TYPE_RENAME_MODULE;
        }else{
            this.type = TYPE_RENAME_UNRESOLVED_IMPORT;
        }
	}
	
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
		addOccurrences(request, getOccurrencesWithScopeAnalyzer(request));
    }

    @Override
    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        checkInitialOnLocalScope(status, request);
    }
}
