/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.Stack;

import org.python.parser.ast.Assign;


public class AssignDefinition extends Definition{
    
    /**
     * Name of the token.
     * 
     * e.g.
     * tok = ClassA()
     * 
     * the value equals ClassA
     */
    public String value;
    
    /**
     * This is the token name.
     */
    public String target;
    
    /**
     * This is the position in the target.
     * 
     * e.g. if we have:
     * 
     * a, b = someCall()
     * 
     * and we're looking for b, target pos would be 1
     * if we were looking for a, target pos would be 0
     */
    public int targetPos;
    
    /**
     * Assign ast.
     */
    public Assign ast;
    
    /**
     * Node with the path of classes / funcs to get to an assign.
     */
    public Stack nodeStack = new Stack(); 
    
    /**
     * Default constructor.
     */
    public AssignDefinition(){
        
    }

    /**
     * Constructor.
     * 
     * @param assign
     * @param ast
     * @param line
     * @param col
     */
    public AssignDefinition(String value, String target, int targetPos, Assign ast, int line, int col){
        super(line, col);
        this.target = target;
        this.targetPos = targetPos;
        this.value = value;
        this.ast = ast;
    }
}