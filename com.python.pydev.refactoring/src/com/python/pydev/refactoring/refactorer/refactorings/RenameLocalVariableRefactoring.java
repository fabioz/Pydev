/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.python.parser.SimpleNode;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class RenameLocalVariableRefactoring extends AbstractRefactoring{
    
    private RefactoringRequest request;
    private SimpleNode ast;

    public RenameLocalVariableRefactoring(RefactoringRequest request) {
        this.request = request;
        ast = request.getAST();
    }
    
    @Override
    public RefactoryChange getRefactoringChange(){
        try {
            RefactoryChange change = new RefactoryChange();

            int line = request.getBeginLine();
            int col = request.getBeginCol();
            
            FindScopeVisitor visitor = new FindScopeVisitor(line, col);
            ast.accept(visitor);
            Scope scope = visitor.scope;
            
            IToken[] localTokens = scope.getAllLocalTokens();
            for (IToken token : localTokens) {
                System.out.println(token.getRepresentation());
                System.out.println(token.getLineColEnd());
            }
            return change;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

    @Override
    public void performRefactoring(RefactoryChange change) {
    }

    
    
}
