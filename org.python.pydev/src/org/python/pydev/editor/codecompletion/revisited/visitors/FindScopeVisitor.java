/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;

/**
 * @author Fabio Zadrozny
 */
public class FindScopeVisitor extends AbstractVisitor {
    
    /**
     * Stack of classes / methods representing the scope.
     */
    private Stack stackScope = new Stack();

    /**
     * This is the scope.
     */
    public Scope scope = new Scope(new Stack());
    
    /**
     * Variable to mark if we found scope.
     */
    private boolean found = false;
    
    /**
     * line to find
     */
    private int line;
    
    /**
     * column to find
     */
    private int col;

    /**
     * Constructor
     * 
     * @param line
     * @param col
     */
    public FindScopeVisitor(int line, int col){
       this.line = line;
       this.col = col;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#unhandled_node(org.python.parser.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        //the line passed in starts at 1 and the lines for the visitor nodes start at 0
        
        if(! found){
	        if(line <= node.beginLine ){
	            //scope is locked at this time.
	            found = true;
	            scope = new Scope(this.stackScope);
	        }
        }
        return null;
    }

    /**
     * @see org.python.parser.ast.VisitorBase#traverse(org.python.parser.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    
    /**
     * @see org.python.parser.ast.VisitorBase#visitClassDef(org.python.parser.ast.ClassDef)
     */
    public Object visitClassDef(ClassDef node) throws Exception {
        if(!found){
	        stackScope.push(node);
	        node.traverse(this);
	        stackScope.pop();
        }
        return null;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#visitFunctionDef(org.python.parser.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if(!found){
	        stackScope.push(node);
	        node.traverse(this);
	        stackScope.pop();
        }
        return null;
    }


}
