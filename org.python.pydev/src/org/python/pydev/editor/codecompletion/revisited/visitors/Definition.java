/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.parser.SimpleNode;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

/**
 * @author Fabio Zadrozny
 */
public class Definition {

    /**
     * Line of the definition.
     */
    public int line;
    
    /**
     * Column of the definition.
     */
    public int col;


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
     * This is the module where the definition is.
     */
    public AbstractModule module;

    /**
     * Assign ast.
     */
    public SimpleNode ast;

    /**
     * Node with the path of classes / funcs to get to an assign.
     */
    public Scope scope;

    
    public Definition(int line, int col, String value, SimpleNode ast, Scope scope, AbstractModule module){
        this.line = line;
        this.col = col;
        this.value = value;
        this.ast = ast;
        this.scope = scope;
        this.module = module;
    }

}
