/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.parser.ast.Assign;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;


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
     * This is the module where the definition is.
     */
    public AbstractModule module;
    
    /**
     * Assign ast.
     */
    public Assign ast;
    
    /**
     * Node with the path of classes / funcs to get to an assign.
     */
    public Scope scope; 
    
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
    public AssignDefinition(String value, String target, int targetPos, Assign ast, int line, int col, Scope scope, AbstractModule module){
        super(line, col);
        this.target = target;
        this.targetPos = targetPos;
        this.value = value;
        this.ast = ast;
        this.scope = scope;
        this.module = module;
    }
}