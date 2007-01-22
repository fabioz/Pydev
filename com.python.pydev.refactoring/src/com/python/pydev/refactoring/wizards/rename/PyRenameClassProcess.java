/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.refactorer.RefactorerRequestConstants;
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
     * When checking the class on a local scope, we have to cover the class definition
     * itself and any access to it (global)
     */
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode root = request.getAST();
        List<ASTEntry> oc;
        if(request.moduleName.equals(definition.module.getName())){
            ASTEntry classDefInAst = getOriginalClassDefInAst(root);
            
            if(classDefInAst == null){
                status.addFatalError("Unable to find the original definition for the class definition.");
                return;
            }
            
            while(classDefInAst.parent != null){
                if(classDefInAst.parent.node instanceof FunctionDef){
                    request.setAdditionalInfo(RefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, true); //it is in a local scope.
                    oc = this.getOccurrencesWithScopeAnalyzer(request);
                    addOccurrences(request, oc);
                    return;
                }
                classDefInAst = classDefInAst.parent;
            }

            //it is defined in the module we're looking for
            oc = this.getOccurrencesWithScopeAnalyzer(request);
        }else{
            //it is defined in some other module
            oc = ScopeAnalysis.getLocalOcurrences(request.initialName, root);
        }
        oc.addAll(ScopeAnalysis.getAttributeReferences(request.initialName, root));
        
		addOccurrences(request, oc);
    }


    /**
     * @param simpleNode this is the module with the AST that has the function definition
     * @return the function definition that matches the original definition as an ASTEntry
     */
    private ASTEntry getOriginalClassDefInAst(SimpleNode simpleNode) {
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(simpleNode);
        Iterator<ASTEntry> it = visitor.getIterator(ClassDef.class);
        ASTEntry classDefEntry = null;
        while(it.hasNext()){
            classDefEntry = it.next();
            
            if(classDefEntry.node.beginLine == this.definition.ast.beginLine && 
                    classDefEntry.node.beginColumn == this.definition.ast.beginColumn){
                return classDefEntry;
            }
        }
        return null;
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
    
    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return true;
    }

}
