/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.Stack;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;

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
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        //the line passed in starts at 1 and the lines for the visitor nodes start at 0
        if(! found && !(node instanceof Module)){
	        if(line <= node.beginLine ){
	            //scope is locked at this time.
	            found = true;
	            int original = scope.ifMainLine;
	            scope = new Scope((Stack<SimpleNode>) this.stackScope.clone());
	            scope.ifMainLine = original;
	        }
        }else{
            if(scope.scopeEndLine == -1 && line < node.beginLine && col >= node.beginColumn){
                scope.scopeEndLine = node.beginLine; 
            }
        }
        return node;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitIf(org.python.pydev.parser.jython.ast.If)
     */
    public Object visitIf(If node) throws Exception {
        if(isIfMAinNode(node)){
            scope.ifMainLine = node.beginLine;
        }
        return super.visitIf(node);
    }
    

    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    public Object visitClassDef(ClassDef node) throws Exception {
        if(!found){
	        stackScope.push(node);
	        node.traverse(this);
	        stackScope.pop();
        }
        return super.visitClassDef(node);
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if(!found){
	        stackScope.push(node);
	        node.traverse(this);
	        stackScope.pop();
        }
        return super.visitFunctionDef(node);
    }


}
