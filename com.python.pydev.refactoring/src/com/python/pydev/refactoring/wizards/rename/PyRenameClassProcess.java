/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.wizards.RefactorProcessFactory;

/**
 * This is the process that should take place when the definition maps to a class
 * definition (its AST is a ClassDef)
 * 
 * @see RefactorProcessFactory#getProcess(Definition) for details on choosing the 
 * appropriate process.
 * 
 * Note that the definition found may map to some module that is not actually
 * the current module, meaning that we may have a RenameClassProcess even
 * if the class definition is on some other module.
 * 
 * Important: the assumptions that can be made given this are:
 * - The current module has the token that maps to the definition found (so, it
 * doesn't need to be double checked)
 * - The module where the definition was found also does not need double checking
 * 
 * - All other modules need double checking if there is some other token in the 
 * workspace with the same name.
 * 
 * @author Fabio
 */
public class PyRenameClassProcess extends AbstractRenameWorkspaceRefactorProcess{

    /**
     * Do we want to debug?
     */
    public static final boolean DEBUG_CLASS_PROCESS = false;
    
    /**
     * Creates the rename class process with a definition.
     * 
     * @param definition a definition with a ClassDef.
     */
    public PyRenameClassProcess(Definition definition) {
        super(definition);
        Assert.isTrue(this.definition.ast instanceof ClassDef);
    }

    /**
     * When checking the class on a local scope, we have to cover the class declaration
     * itself and any access to it (global)
     * 
     * @see com.python.pydev.refactoring.wizards.rename.AbstractRenameRefactorProcess#checkInitialOnLocalScope(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        SimpleNode root = request.getAST();
        
        List<ASTEntry> oc = ScopeAnalysis.getLocalOcurrences(request.duringProcessInfo.initialName, root);
        oc.addAll(ScopeAnalysis.getAttributeReferences(request.duringProcessInfo.initialName, root));
        
		addOccurrences(request, oc);
    }

    /**
     * This method is called for each module that may have some reference to the definition
     * we're looking for. 
     */
    protected List<ASTEntry> getEntryOccurrences(RefactoringStatus status, String initialName, SourceModule module) {
        List<ASTEntry> entryOccurrences = ScopeAnalysis.getLocalOcurrences(initialName, module.getAst());
        entryOccurrences.addAll(ScopeAnalysis.getAttributeReferences(initialName, module.getAst()));
        return entryOccurrences;
    }
}
