/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.refactorer.RefactorerFindReferences;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * This is the process that should take place when the definition maps to a class
 * definition (its AST is a ClassDef)
 * 
 * @see RefactorProcessFactory#getProcess(Definition) for details on choosing the 
 * appropriate process.
 * 
 * @author Fabio
 */
public class PyRenameClassProcess extends AbstractRenameRefactorProcess{

    public PyRenameClassProcess(Definition definition) {
        super(definition);
    }

    /**
     * When checking the class on a local scope, we have to cover the class declaration
     * itself and any access to it (global)
     * 
     * 
     * @see com.python.pydev.refactoring.wizards.rename.AbstractRenameRefactorProcess#checkInitialOnLocalScope(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        SimpleNode root = request.getAST();
        List<ASTEntry> oc = ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, root);
        addOccurrences(request, oc);
        oc = ScopeAnalysis.getAttributeReferences(request.duringProcessInfo.initialName, root);
		addOccurrences(request, oc);
    }
    
    /**
     * For files that do not have the class declaration, we have to get:
     * @see com.python.pydev.refactoring.wizards.rename.AbstractRenameRefactorProcess#checkInitialOnWorkspace(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        try{
            checkInitialOnLocalScope(status, request);
            
            List<IFile> references = new RefactorerFindReferences().findPossibleReferences(request);
            for (IFile file : references) {
                IProject project = file.getProject();
                PythonNature nature = PythonNature.getPythonNature(project);
                if(nature != null){
                    ProjectModulesManager modulesManager = (ProjectModulesManager) nature.getAstManager().getModulesManager();
                    String modName = modulesManager.resolveModuleInDirectManager(file, project);
                    if(modName != null){
                        if(!request.moduleName.equals(modName)){
                            //we've already checked the module from the request...
                            SourceModule module = (SourceModule) nature.getAstManager().getModule(modName, nature, true, false);
                            
                            List<ASTEntry> entryOccurrences = ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, module.getAst());
                            addOccurrences(entryOccurrences, file, modName);
                            
                            entryOccurrences = ScopeAnalysis.getAttributeReferences(request.duringProcessInfo.initialName, module.getAst());
                            addOccurrences(entryOccurrences, file, modName);
                        }
                    }
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
