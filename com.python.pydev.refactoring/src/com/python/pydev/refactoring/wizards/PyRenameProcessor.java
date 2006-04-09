/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

/**
 * Rename to a local variable...
 * 
 * Straightforward 'way': - find the definition and assert it is not a global - rename all occurences within that scope
 * 
 * 'Blurred things': - if we have something as:
 * 
 * case 1: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         a = 3 
 *         print a 
 *     print a
 * 
 * case 2: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         print a 
 *         a = 3 
 *         print a 
 *     print a
 * 
 * case 3: 
 * 
 * def m1(): 
 *     a = 1 
 *     def m2(): 
 *         if foo: 
 *             a = 3 
 *         print a 
 *     print a
 * 
 * if we rename it inside of m2, do we have to rename it in scope m1 too? what about renaming it in m1?
 * 
 * The solution that will be implemented will be:
 * 
 *  - if we rename it inside of m2, it will only rename inside of its scope in any case 
 *  (the problem is when the rename request commes from an 'upper' scope).
 *  
 *  - if we rename it inside of m1, it will rename it in m1 and m2 only if it is used inside 
 *  that scope before an assign this means that it will rename in m2 in case 2 and 3, but not in case 1.
 */
public class PyRenameProcessor extends RenameProcessor {

    private RefactoringRequest request;

    private TextChange fChange;

    private PyRenameLocalProcess process;

    public PyRenameProcessor(RefactoringRequest request) {
        this.request = request;
    }

    @Override
    public Object[] getElements() {
        return new Object[] { this.request };
    }

    public static final String IDENTIFIER = "org.python.pydev.pyRenameLocalVariable";

    public static final boolean DEBUG = true;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getProcessorName() {
        return "Pydev PyRenameProcessor";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return true;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        if(! DocUtils.isWord(request.duringProcessInfo.initialName)){
            status.addFatalError("The initial name is not valid:"+request.duringProcessInfo.initialName);
            return status;
        }
        
        SimpleNode ast = request.getAST();
        if(ast == null){
            status.addFatalError("AST not generated (syntax error).");
            return status;
        }
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        ItemPointer[] pointers = pyRefactoring.findDefinition(request);
        
        if(pointers.length == 0){
            status.addFatalError("Unable to find the definition of the variable to rename.");
            return status;
        }
        
        if(pointers.length > 1){
            StringBuffer buffer = new StringBuffer("Too many definitions found for the variable to rename:");
            for (ItemPointer pointer : pointers) {
                buffer.append(pointer);
                buffer.append("\n");
            }
            status.addFatalError(buffer.toString());
            return status;
        }
        
        ItemPointer pointer = pointers[0];
        if(pointer.definition == null){
            status.addFatalError("The definition found is not valid. "+pointer);
        }
        if(DEBUG){
            System.out.println("Found:"+pointer.definition);
        }
        
        process = RefactorProcessFactory.getProcess(pointer.definition);
        process.checkInitialConditions(pm, status, this.request);
        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        fChange = new PyRenameChange(pm, request);
        if(process == null){
            status.addFatalError("Refactoring Process not defined: the pre-conditions were not satisfied.");
        }else{
            process.checkFinalConditions(pm, context, status, fChange);
        }
        return status;
    }


    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return fChange;
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
        return null; // no participants are loaded
    }


}
