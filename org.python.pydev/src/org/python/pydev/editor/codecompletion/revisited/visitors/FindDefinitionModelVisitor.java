/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitor extends AbstractVisitor{

    /**
     * This is the token to find.
     */
    private String tokenToFind;
    
    /**
     * List of definitions.
     */
    public List definitions = new ArrayList();
    
    /**
     * Stack of classes / methods to get to a definition.
     */
    private Stack defsStack = new Stack();
    
    private AbstractModule module;
    
    /**
     * Constructor 
     * 
     * @param token
     * @param line
     * @param col
     */
    public FindDefinitionModelVisitor(String token, int line, int col, AbstractModule module){
        this.tokenToFind = token;
        this.module = module;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#unhandled_node(org.python.parser.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
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
        defsStack.push(node);
        node.traverse(this);
        defsStack.pop();
        return null;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#visitFunctionDef(org.python.parser.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        defsStack.push(node);
        node.traverse(this);
        defsStack.pop();
        return null;
    }

    /**
     * @see org.python.parser.ast.VisitorBase#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        
        for (int i = 0; i < node.targets.length; i++) {
            String rep = NodeUtils.getFullRepresentationString(node.targets[i]);
	        
            if(rep != null && rep.equals(tokenToFind)){
	            String value = NodeUtils.getFullRepresentationString(node.value);
	            
	            AssignDefinition definition = new AssignDefinition(value, rep, i, node, node.beginLine, node.beginColumn, new Scope(this.defsStack), module);
	            definitions.add(definition);
	        }
        }
        
        return null;
    }
}
