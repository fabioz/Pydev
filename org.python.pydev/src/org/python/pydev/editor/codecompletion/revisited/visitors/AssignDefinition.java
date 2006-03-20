/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.ast.Assign;


public class AssignDefinition extends Definition{
    
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
     * Constructor.
     * 
     * @param assign
     * @param ast
     * @param line
     * @param col
     */
    public AssignDefinition(String value, String target, int targetPos, Assign ast, int line, int col, Scope scope, IModule module){
        super(line, col, value, ast, scope, module);
        this.target = target;
        this.targetPos = targetPos;
    }
}