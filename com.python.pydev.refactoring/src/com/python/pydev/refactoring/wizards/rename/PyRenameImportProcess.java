package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.analysis.visitors.Found;
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
        //now, on the workspace, we need to find the module definition as well as the imports for it...
        //the local scope should have already determined which is the module to be renamed (unless it
        //is an unresolved import, in which case we'll only make a local refactor)
        if(docOccurrences.size() != 0){
            ASTEntry entry = docOccurrences.get(0);
            Found found = (Found) entry.getAdditionalInfo(ScopeAnalyzerVisitor.FOUND_ADDITIONAL_INFO_IN_AST_ENTRY, null);
            if(found == null){
                throw new RuntimeException("Expecting decorated entry.");
            }
            if(found.importInfo == null){
                throw new RuntimeException("Expecting import info from the found entry.");
            }
            if(found.importInfo.wasResolved){
                SourceModule mod = (SourceModule) found.importInfo.mod;
                IFile workspaceFile = null; 
                try{
                    workspaceFile = PydevPlugin.getWorkspaceFile(mod.getFile());
                }catch(IllegalStateException e){
                    //this can happen on tests (but if not on tests, we want to re-throw it
                    if(!e.getMessage().equals("Workspace is closed.")){
                        throw e; 
                    }
                    //otherwise, let's just keep going in the test and add it as a valid entry
                }
                
                List<ASTEntry> lst = new ArrayList<ASTEntry>();
                lst.add(new ASTEntryWithSourceModule(mod));
                addOccurrences(lst, workspaceFile, mod.getName());
            }
        }
    }
}
