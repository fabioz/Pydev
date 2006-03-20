/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.visitors.OcurrencesVisitor;

/**
 * Rename to a local variable...
 * 
 * Straightforward 'way':
 * - find the definition and assert it is not a global
 * - rename all occurences within that scope
 * 
 * 'Blurred things':
 * - if we have something as:
 * 
 * case 1:
 * def m1():
 *     a = 1
 *     def m2():
 *         a = 3
 *         print a
 *     print a
 *     
 * case 2:
 * def m1():
 *     a = 1
 *     def m2():
 *         print a
 *         a = 3
 *         print a
 *     print a
 *     
 * case 3:
 * def m1():
 *     a = 1
 *     def m2():
 *         if foo:
 *             a = 3
 *         print a
 *     print a
 *     
 * if we rename it inside of m2, do we have to rename it in scope m1 too?
 * what about renaming it in m1?
 * 
 * The solution that will be implemented will be:
 * 
 * - if we rename it inside of m2, it will only rename inside of its scope in any case (the problem is when the 
 * rename request commes from an 'upper' scope).
 *  
 * - if we rename it inside of m1, it will rename it in m1 and m2 only if it is used inside that scope before an assign
 * this means that it will rename in m2 in case 2 and 3, but not in case 1.
 */
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
            int col = request.getBeginCol()+1;
            
            //TODO: instead of using this 'simple' scope visitor, we should extend the OcurrencesVisitor to 
            //lend us a 'complete' scope.
//            request.getModule().findDefinition()
            
            
            FindScopeVisitor visitor = new FindScopeVisitor(line, col);
            ast.accept(visitor);
            Scope scope = visitor.scope;
            
            IToken[] localTokens = scope.getAllLocalTokens();
            for (IToken token : localTokens) {
                System.out.println(token.getRepresentation());
                if(token.getLineDefinition() == line){
                	if(isInside(col,token.getColDefinition(), token.getLineColEnd()[1])){
                		System.out.println("Found:"+token);
                	}
                }
            }
            return change;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

	private boolean isInside(int col, int colDefinition, int endColDefinition) {
		return col >= colDefinition && col <= endColDefinition;
	}


    
    
}
