/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.util.Assert;
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
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;

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
public class PyRenameLocalVariableProcessor extends RenameProcessor {

    private RefactoringRequest request;

    private TextChange fChange;

    public PyRenameLocalVariableProcessor(RefactoringRequest request) {
        this.request = request;
    }

    private boolean isInside(int col, int colDefinition, int endColDefinition) {
        return col >= colDefinition && col <= endColDefinition;
    }

    @Override
    public Object[] getElements() {
        return new Object[] { this.request };
    }

    public static final String IDENTIFIER = "org.python.pydev.pyRenameLocalVariable";

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getProcessorName() {
        return "Pydev ProcessorName";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return true;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        SimpleNode ast = request.getAST();
        if(true){
            status.addFatalError("AST not generated (syntax error).");
            return status;
        }
        int line = request.getBeginLine();
        int col = request.getBeginCol() + 1;
        
        FindScopeVisitor visitor = new FindScopeVisitor(line, col);
        try {
            ast.accept(visitor);
            Scope scope = visitor.scope;

            IToken[] localTokens = scope.getAllLocalTokens();
            for (IToken token : localTokens) {
                if (token.getLineDefinition() == line) {
                    if (isInside(col, token.getColDefinition(), token.getLineColEnd()[1])) {
                        System.out.println(scope.scope.peek());
                        System.out.println(token);
                    }
                }
            }
        } catch (Exception e) {
            status.addError(e.getMessage());
        }
        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        fChange = new PyRenameChange(pm, request);

        MultiTextEdit rootEdit = new MultiTextEdit();
        fChange.setEdit(rootEdit);
        fChange.setKeepPreviewEdits(true);

        TextEdit declarationEdit = createRenameEdit(request.duringProcessInfo.initialOffset);
        for (TextEdit t : getAllRenameEdits(declarationEdit)) {
            rootEdit.addChild(t);
            fChange.addTextEditGroup(new TextEditGroup("changeName", t));
        }
        return new RefactoringStatus();
    }

    private TextEdit[] getAllRenameEdits(TextEdit declarationEdit) {
        return new TextEdit[] { declarationEdit };
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        Assert.isNotNull(request.duringProcessInfo.name);
        return fChange;
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
        return null; // no participants are loaded
    }

    private TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }

}
